import tempfile
import unittest
from pathlib import Path

from ml.benchmark.benchmark_tflite import summarize
from ml.evaluate import f1_score, to_markdown
from ml.export_tflite import find_int8_tflite
from ml.train import IMG_SIZE, build_train_kwargs
from ml.verify_tflite import check_contract


class TrainKwargsTest(unittest.TestCase):
    def kwargs(self):
        return build_train_kwargs(Path("data.yaml"), epochs=10, batch=8, project=Path("runs"), name="x")

    def test_input_size_matches_the_app(self):
        self.assertEqual(320, IMG_SIZE)
        self.assertEqual(320, self.kwargs()["imgsz"])

    def test_no_vertical_flip_because_inverted_containers_are_a_class(self):
        self.assertEqual(0.0, self.kwargs()["flipud"])

    def test_training_is_reproducible(self):
        k = self.kwargs()
        self.assertTrue(k["deterministic"])
        self.assertEqual(0, k["seed"])


class EvaluateHelpersTest(unittest.TestCase):
    def test_f1(self):
        self.assertAlmostEqual(0.5, f1_score(0.5, 0.5))
        self.assertEqual(0.0, f1_score(0.0, 0.0))
        self.assertAlmostEqual(2 * 0.9 * 0.6 / 1.5, f1_score(0.9, 0.6))

    def test_markdown_contains_all_rows(self):
        md = to_markdown(
            dict(precision=0.8, recall=0.7, f1=0.75, map50=0.6, map50_95=0.4),
            [dict(name="bucket:water", precision=0.9, recall=0.8, f1=0.85, ap50=0.7)],
        )
        self.assertIn("bucket:water", md)
        self.assertIn("| mAP@0.5 | 0.600 |", md)


class ExportHelpersTest(unittest.TestCase):
    def test_finds_int8_model_in_saved_model_dir(self):
        with tempfile.TemporaryDirectory() as d:
            sub = Path(d) / "best_saved_model"
            sub.mkdir()
            (sub / "best_float32.tflite").write_bytes(b"x")
            (sub / "best_int8.tflite").write_bytes(b"x")
            self.assertEqual("best_int8.tflite", find_int8_tflite(Path(d)).name)

    def test_returns_none_when_missing(self):
        with tempfile.TemporaryDirectory() as d:
            self.assertIsNone(find_int8_tflite(Path(d)))


class ContractTest(unittest.TestCase):
    def fatal_failures(self, *args):
        return [c.name for c in check_contract(*args) if not c.ok and c.fatal]

    def test_valid_model(self):
        self.assertEqual([], self.fatal_failures([1, 320, 320, 3], [[1, 14, 2100]], 10, 3.2))

    def test_channel_last_output_is_also_valid(self):
        self.assertEqual([], self.fatal_failures([1, 320, 320, 3], [[1, 2100, 14]], 10, 3.2))

    def test_wrong_class_count_fails(self):
        self.assertTrue(self.fatal_failures([1, 320, 320, 3], [[1, 84, 2100]], 10, 3.2))

    def test_non_square_or_non_rgb_input_fails(self):
        self.assertTrue(self.fatal_failures([1, 320, 256, 3], [[1, 14, 2100]], 10, 3.2))
        self.assertTrue(self.fatal_failures([1, 320, 320, 1], [[1, 14, 2100]], 10, 3.2))

    def test_embedded_nms_with_several_outputs_fails(self):
        self.assertTrue(self.fatal_failures([1, 320, 320, 3], [[1, 14, 2100], [1, 5]], 10, 3.2))

    def test_large_model_is_only_a_warning(self):
        checks = check_contract([1, 320, 320, 3], [[1, 14, 2100]], 10, 40.0)
        size = [c for c in checks if "15 MB" in c.name][0]
        self.assertFalse(size.ok)
        self.assertFalse(size.fatal)


class BenchmarkSummaryTest(unittest.TestCase):
    def test_percentiles_match_the_app_method(self):
        s = summarize([float(i * 10) for i in range(1, 21)])  # 10..200, igual que LatencyStatsTest.kt
        self.assertEqual(100.0, s["p50"])
        self.assertEqual(190.0, s["p95"])


if __name__ == "__main__":
    unittest.main()
