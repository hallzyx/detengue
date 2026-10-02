import tempfile
import unittest
from pathlib import Path

from ml.import_external import (build_id_map, convert_label_text, import_source, parse_mapping,
                                read_source_names)

TARGET = ["bucket", "bottle", "tire"]


class ImportExternalTest(unittest.TestCase):
    def test_names_inline_and_multiline(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "data.yaml"
            p.write_text("nc: 2\nnames: ['Bottle', 'Bucket']\n", encoding="utf-8")
            self.assertEqual(["Bottle", "Bucket"], read_source_names(p))
            p.write_text("train: x\nnames:\n  0: Pool\n  1: Tire\nroboflow:\n  a: b\n", encoding="utf-8")
            self.assertEqual(["Pool", "Tire"], read_source_names(p))
            p.write_text("names:\n- Pool\n- Tire\n", encoding="utf-8")
            self.assertEqual(["Pool", "Tire"], read_source_names(p))

    def test_mapping_and_idmap(self):
        m = parse_mapping("Bucket=bucket, Tire=tire")
        self.assertEqual({0: 0, 2: 2}, build_id_map(["Bucket", "Pool", "Tire"], m, TARGET))
        with self.assertRaises(ValueError):
            build_id_map(["Bucket"], parse_mapping("Bucket=banana"), TARGET)
        with self.assertRaises(ValueError):
            build_id_map(["Bucket"], parse_mapping("Tire=tire"), TARGET)

    def test_convert_drops_image_with_unmapped_or_polygon(self):
        idm = {0: 2}
        self.assertEqual(["2 0.5 0.5 0.1 0.1"], convert_label_text("0 0.5 0.5 0.1 0.1\n", idm))
        self.assertEqual([], convert_label_text("", idm))
        self.assertIsNone(convert_label_text("0 .5 .5 .1 .1\n1 .5 .5 .1 .1\n", idm))
        self.assertIsNone(convert_label_text("0 .1 .1 .2 .2 .3 .3\n", idm))

    def test_import_end_to_end(self):
        with tempfile.TemporaryDirectory() as d:
            src, dst = Path(d) / "src", Path(d) / "dst"
            (src / "valid" / "images").mkdir(parents=True)
            (src / "valid" / "labels").mkdir(parents=True)
            (src / "data.yaml").write_text("names: ['Pool', 'Tire']\n", encoding="utf-8")
            for stem, lbl in (("a", "1 .5 .5 .2 .2\n"), ("b", "0 .5 .5 .2 .2\n"), ("c", "")):
                (src / "valid" / "images" / f"{stem}.jpg").write_bytes(b"x")
                (src / "valid" / "labels" / f"{stem}.txt").write_text(lbl, encoding="utf-8")
            st = import_source(src, "s", parse_mapping("Tire=tire"), dst, TARGET)
            self.assertEqual((2, 1, 1), (st.copied, st.negatives, st.skipped_unmapped))
            self.assertEqual({"tire": 1}, st.boxes)
            self.assertEqual("2 .5 .5 .2 .2\n", (dst / "labels" / "val" / "s_a.txt").read_text())
            self.assertTrue((dst / "images" / "val" / "s_c.jpg").exists())


if __name__ == "__main__":
    unittest.main()
