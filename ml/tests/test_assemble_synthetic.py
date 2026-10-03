import tempfile
import unittest
from pathlib import Path

from ml.assemble_synthetic import assemble


class AssembleSyntheticTest(unittest.TestCase):
    def test_only_train_and_negatives_empty(self):
        with tempfile.TemporaryDirectory() as tmp:
            t = Path(tmp)
            (t / "lab" / "bucket_open").mkdir(parents=True)
            (t / "neg").mkdir()
            (t / "lab" / "bucket_open" / "a.jpg").write_bytes(b"x")
            (t / "lab" / "bucket_open" / "a.txt").write_text("0 0.5 0.5 0.2 0.2\n")
            (t / "lab" / "bucket_open" / "huerfana.jpg").write_bytes(b"x")  # sin .txt: se ignora
            (t / "neg" / "n.jpg").write_bytes(b"x")
            counts = assemble(t / "lab", t / "neg", t / "ds")
            self.assertEqual(counts, {"con_caja": 1, "negativas": 1})
            self.assertTrue((t / "ds" / "images" / "train" / "syn_a.jpg").is_file())
            self.assertEqual((t / "ds" / "labels" / "train" / "syn_n.txt").read_text(), "")
            for split in ("val", "test"):
                self.assertFalse(list((t / "ds" / "images" / split).glob("*")))

    def test_idempotent(self):
        with tempfile.TemporaryDirectory() as tmp:
            t = Path(tmp)
            (t / "lab" / "x").mkdir(parents=True)
            (t / "lab" / "x" / "a.jpg").write_bytes(b"x")
            (t / "lab" / "x" / "a.txt").write_text("0 0.5 0.5 0.2 0.2\n")
            assemble(t / "lab", t / "neg", t / "ds")
            assemble(t / "lab", t / "neg", t / "ds")
            self.assertEqual(len(list((t / "ds" / "images" / "train").glob("syn_*"))), 1)

    def test_degraded_optional(self):
        with tempfile.TemporaryDirectory() as tmp:
            t = Path(tmp)
            (t / "deg" / "bucket_open").mkdir(parents=True)
            (t / "deg" / "negativos").mkdir()
            (t / "deg" / "bucket_open" / "a_deg.jpg").write_bytes(b"x")
            (t / "deg" / "bucket_open" / "a_deg.txt").write_text("0 0.5 0.5 0.2 0.2\n")
            (t / "deg" / "negativos" / "n_deg.jpg").write_bytes(b"x")
            sin = assemble(t / "lab", t / "neg", t / "ds")
            self.assertNotIn("degradadas_con_caja", sin)
            con = assemble(t / "lab", t / "neg", t / "ds", t / "deg")
            self.assertEqual((con["degradadas_con_caja"], con["degradadas_negativas"]), (1, 1))
            self.assertEqual((t / "ds" / "labels" / "train" / "syn_n_deg.txt").read_text(), "")


if __name__ == "__main__":
    unittest.main()
