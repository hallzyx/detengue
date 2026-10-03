import tempfile
import unittest
from pathlib import Path

from ml.assemble_web_dataset import assemble
from ml.classes import load_labels
from ml.webdata import (STATE_PLAN, accept_result, attribution_row, build_search_params, pick_best,
                        safe_name, split_for_stem, xyxy_to_yolo)


class WebDataTest(unittest.TestCase):
    def test_plan_states_exist_in_states_label_file(self):
        names = [c.name for c in load_labels(Path(__file__).resolve().parents[1] / "labels_mvp_states.txt")]
        for state in STATE_PLAN:
            self.assertIn(state, names)

    def test_license_and_extension_filter(self):
        ok = {"url": "https://x/a.jpg?w=1", "license": "by"}
        self.assertTrue(accept_result(ok))
        self.assertFalse(accept_result({**ok, "license": "by-nc"}))
        self.assertFalse(accept_result({**ok, "license": "by-nd"}))
        self.assertFalse(accept_result({"url": "https://x/a.gif", "license": "cc0"}))
        self.assertFalse(accept_result({"license": "cc0"}))

    def test_search_params_only_open_licenses(self):
        p = build_search_params("empty bucket", 2)
        self.assertEqual("cc0,pdm,by,by-sa", p["license"])
        self.assertEqual("false", p["mature"])

    def test_names_and_attribution(self):
        self.assertEqual("a_b_c", safe_name("a/b\\c"))
        row = attribution_row({"id": "i/1", "title": "t", "creator": "c", "license": "by", "license_version": "2.0",
                               "foreign_landing_url": "u"}, "bucket:water", "q")
        self.assertEqual(["i_1", "bucket:water", "q", "t", "c", "by 2.0", "u"], row)

    def test_xyxy_to_yolo_and_clipping(self):
        self.assertEqual((0.5, 0.5, 0.5, 0.5), xyxy_to_yolo((25, 25, 75, 75), 100, 100))
        cx, cy, w, h = xyxy_to_yolo((-10, 0, 50, 120), 100, 100)
        self.assertEqual((0.25, 0.5, 0.5, 1.0), (cx, cy, w, h))

    def test_pick_best_filters_score_and_area(self):
        boxes = [(0, 0, 100, 100), (10, 10, 60, 60), (0, 0, 5, 5)]
        self.assertEqual(((10, 10, 60, 60), 0.6), pick_best(boxes, [0.2, 0.6, 0.9], 100, 100))  # 1º score bajo, 3º área mínima
        self.assertIsNone(pick_best([(0, 0, 100, 100)], [0.9], 100, 100))  # 100 % del cuadro: descartada

    def test_split_is_deterministic_and_roughly_80_10_10(self):
        self.assertEqual(split_for_stem("web_x"), split_for_stem("web_x"))
        from collections import Counter
        c = Counter(split_for_stem(f"web_{i}") for i in range(3000))
        self.assertTrue(2250 < c["train"] < 2550 and 200 < c["val"] < 400 and 200 < c["test"] < 400, c)

    def test_assemble_ignores_unpaired_files(self):
        with tempfile.TemporaryDirectory() as d:
            src, dst = Path(d) / "s", Path(d) / "d"
            (src / "bucket_water").mkdir(parents=True)
            for n in ("ok", "no_label"):
                (src / "bucket_water" / f"{n}.jpg").write_bytes(b"x")
            (src / "bucket_water" / "ok.txt").write_text("1 .5 .5 .2 .2\n")
            (src / "bucket_water" / "deleted_image.txt").write_text("1 .5 .5 .2 .2\n")
            self.assertEqual(1, sum(assemble(src, dst).values()))
            self.assertEqual(1, len(list(dst.rglob("*.txt"))))


if __name__ == "__main__":
    unittest.main()
