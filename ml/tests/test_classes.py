import tempfile
import unittest
from pathlib import Path

from ml.classes import LABELS_FILE, ModelClass, load_labels, parse_label


class ParseLabelTest(unittest.TestCase):
    def test_bare_object_has_no_state(self):
        self.assertEqual(ModelClass("bucket", None), parse_label("bucket"))

    def test_object_and_state(self):
        self.assertEqual(ModelClass("bucket", "water"), parse_label("bucket:water"))
        self.assertEqual(ModelClass("bottle", "closed"), parse_label("  bottle : closed  "))

    def test_unknown_object_or_state_is_none(self):
        for bad in ("banana", "bucket:flying", "bucket:", ""):
            self.assertIsNone(parse_label(bad), bad)

    def test_name_roundtrip(self):
        self.assertEqual("bucket:water", parse_label("bucket:water").name)
        self.assertEqual("tire", parse_label("tire").name)


class LoadLabelsTest(unittest.TestCase):
    def test_shipped_labels_file_is_valid(self):
        classes = load_labels(LABELS_FILE)
        self.assertGreaterEqual(len(classes), 2)
        self.assertEqual(len(classes), len({c.name for c in classes}))

    def test_order_follows_file_and_skips_comments(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "labels.txt"
            p.write_text("# comentario\n\nbottle:closed\nbucket:water\n", encoding="utf-8")
            self.assertEqual(["bottle:closed", "bucket:water"], [c.name for c in load_labels(p)])

    def test_training_is_strict_about_invalid_lines(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "labels.txt"
            p.write_text("bucket:water\nbanana\n", encoding="utf-8")
            with self.assertRaises(ValueError):
                load_labels(p)

    def test_duplicates_are_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            p = Path(d) / "labels.txt"
            p.write_text("bucket:water\nbucket:water\n", encoding="utf-8")
            with self.assertRaises(ValueError):
                load_labels(p)


if __name__ == "__main__":
    unittest.main()
