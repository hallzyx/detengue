import tempfile
import unittest
from pathlib import Path

from ml.make_data_yaml import build_yaml
from ml.validate_dataset import validate


def make_dataset(root: Path, files: dict[str, tuple[bytes, str | None]]):
    """files: 'split/nombre.jpg' -> (bytes_imagen, texto_etiqueta | None si no hay .txt)."""
    for rel, (content, label) in files.items():
        split, name = rel.split("/")
        img = root / "images" / split / name
        img.parent.mkdir(parents=True, exist_ok=True)
        img.write_bytes(content)
        lbl_dir = root / "labels" / split
        lbl_dir.mkdir(parents=True, exist_ok=True)
        if label is not None:
            (lbl_dir / f"{Path(name).stem}.txt").write_text(label)


GOOD = "0 0.5 0.5 0.4 0.4\n"


class ValidateDatasetTest(unittest.TestCase):
    def run_validate(self, files, num_classes=2):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            make_dataset(root, files)
            return validate(root, num_classes)

    def valid_files(self):
        return {
            "train/a.jpg": (b"a", GOOD),
            "train/neg.jpg": (b"n", ""),  # negativa
            "val/b.jpg": (b"b", GOOD),
            "test/c.jpg": (b"c", GOOD),
        }

    def test_valid_dataset_has_no_errors_and_counts_negatives(self):
        r = self.run_validate(self.valid_files())
        self.assertTrue(r.ok, r.errors)
        self.assertEqual(1, r.negatives["train"])
        self.assertEqual(2, r.images["train"])

    def test_missing_split_is_an_error(self):
        files = self.valid_files()
        del files["test/c.jpg"]
        r = self.run_validate(files)
        self.assertFalse(r.ok)

    def test_image_without_label_file_is_an_error(self):
        files = self.valid_files()
        files["train/a.jpg"] = (b"a", None)
        self.assertFalse(self.run_validate(files).ok)

    def test_class_out_of_range(self):
        files = self.valid_files()
        files["train/a.jpg"] = (b"a", "5 0.5 0.5 0.4 0.4\n")
        self.assertFalse(self.run_validate(files).ok)

    def test_pixel_coordinates_are_rejected(self):
        files = self.valid_files()
        files["train/a.jpg"] = (b"a", "0 320 240 100 100\n")
        r = self.run_validate(files)
        self.assertFalse(r.ok)
        self.assertTrue(any("0..1" in e for e in r.errors))

    def test_wrong_number_of_values(self):
        files = self.valid_files()
        files["train/a.jpg"] = (b"a", "0 0.5 0.5 0.4\n")
        self.assertFalse(self.run_validate(files).ok)

    def test_identical_image_in_train_and_test_is_data_leakage(self):
        files = self.valid_files()
        files["test/c.jpg"] = (b"a", GOOD)  # mismos bytes que train/a.jpg
        r = self.run_validate(files)
        self.assertFalse(r.ok)
        self.assertTrue(any("fuga" in e for e in r.errors))

    def test_warns_when_there_are_no_negatives(self):
        files = self.valid_files()
        files["train/neg.jpg"] = (b"n", GOOD)
        r = self.run_validate(files)
        self.assertTrue(any("negativas" in w for w in r.warnings))

    def test_warns_about_rare_classes(self):
        r = self.run_validate(self.valid_files())
        self.assertTrue(any("Clase 1" in w for w in r.warnings))


class DataYamlTest(unittest.TestCase):
    def test_yaml_lists_classes_in_order(self):
        text = build_yaml(Path("/data/detengue"), ["bucket:open", "bottle:closed"])
        self.assertIn("nc: 2", text)
        self.assertIn("  0: 'bucket:open'", text)
        self.assertIn("  1: 'bottle:closed'", text)
        self.assertIn("train: images/train", text)


if __name__ == "__main__":
    unittest.main()
