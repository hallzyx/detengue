"""Mismos vectores que YoloPostprocessorTest.kt / StateAwareDetectionTest.kt / LetterboxTransformTest.kt:
si Python y Kotlin discrepan, uno de los dos está mal."""
import unittest

import numpy as np

from ml.classes import ModelClass
from ml.postprocess import Letterbox, postprocess

IDENTITY = Letterbox(320, 320, 320)
BUCKET_BOTTLE = [ModelClass("bucket"), ModelClass("bottle")]


def channel_first(*anchors):
    return np.array(anchors, dtype=np.float32).T[np.newaxis]  # [1, C, N]


def channel_last(*anchors):
    return np.array(anchors, dtype=np.float32)[np.newaxis]  # [1, N, C]


SAMPLE = [
    (0.5, 0.5, 0.4, 0.4, 0.90, 0.10),  # balde
    (0.51, 0.5, 0.4, 0.4, 0.80, 0.05),  # duplicado del balde
    (0.2, 0.2, 0.1, 0.1, 0.05, 0.70),  # botella
]


class LetterboxTest(unittest.TestCase):
    def test_portrait_is_padded_horizontally(self):
        t = Letterbox(480, 640, 320)
        self.assertEqual((240, 320), (t.scaled_w, t.scaled_h))
        self.assertEqual((40.0, 0.0), (t.pad_x, t.pad_y))

    def test_unmap_edges(self):
        t = Letterbox(480, 640, 320)
        self.assertAlmostEqual(0.0, t.unmap_x(40), places=4)
        self.assertAlmostEqual(0.5, t.unmap_x(160), places=4)
        self.assertAlmostEqual(1.0, t.unmap_x(280), places=4)
        self.assertEqual(0.0, t.unmap_x(0))  # dentro del relleno: se recorta

    def test_rounds_half_up_like_kotlin(self):
        # 5 * 0.5 = 2.5: Kotlin roundToInt() -> 3; round() de Python (a par) daría 2.
        self.assertEqual(3, Letterbox(src_w=5, src_h=10, dst=5).scaled_w)


class PostprocessTest(unittest.TestCase):
    def test_decodes_and_suppresses_duplicates(self):
        res = postprocess(channel_first(*SAMPLE), BUCKET_BOTTLE, 320, IDENTITY)
        self.assertEqual(2, len(res))
        self.assertEqual("bucket", res[0].obj)
        self.assertAlmostEqual(0.90, res[0].confidence, places=4)
        self.assertAlmostEqual(0.3, res[0].x, places=3)
        self.assertAlmostEqual(0.4, res[0].w, places=3)
        self.assertEqual("bottle", res[1].obj)

    def test_channel_last_gives_same_result(self):
        a = postprocess(channel_first(*SAMPLE), BUCKET_BOTTLE, 320, IDENTITY)
        b = postprocess(channel_last(*SAMPLE), BUCKET_BOTTLE, 320, IDENTITY)
        self.assertEqual(a, b)

    def test_pixel_coordinates(self):
        res = postprocess(channel_first((160, 160, 128, 128, 0.9, 0.1)), BUCKET_BOTTLE, 320, IDENTITY)
        self.assertEqual(1, len(res))
        self.assertAlmostEqual(0.3, res[0].x, places=3)
        self.assertAlmostEqual(0.4, res[0].w, places=3)

    def test_below_threshold_is_dropped(self):
        res = postprocess(channel_first((0.5, 0.5, 0.4, 0.4, 0.3, 0.2)), BUCKET_BOTTLE, 320, IDENTITY)
        self.assertEqual([], res)

    def test_ignored_classes(self):
        res = postprocess(
            channel_first((0.5, 0.5, 0.4, 0.4, 0.1, 0.95)), [ModelClass("bucket"), None], 320, IDENTITY
        )
        self.assertEqual([], res)

    def test_undoes_letterbox_for_portrait(self):
        res = postprocess(
            channel_first((160, 160, 80, 160, 0.9, 0.1)), BUCKET_BOTTLE, 320, Letterbox(480, 640, 320)
        )
        self.assertEqual(1, len(res))
        self.assertAlmostEqual(1 / 3, res[0].x, places=3)
        self.assertAlmostEqual(1 / 3, res[0].w, places=3)
        self.assertAlmostEqual(0.25, res[0].y, places=3)
        self.assertAlmostEqual(0.5, res[0].h, places=3)

    def test_rejects_shape_that_does_not_match_class_count(self):
        with self.assertRaises(ValueError):
            postprocess(np.zeros((1, 10, 10), dtype=np.float32), BUCKET_BOTTLE, 320, IDENTITY)


class StateAwareTest(unittest.TestCase):
    CLASSES = [ModelClass("bucket", "open"), ModelClass("bucket", "water"), ModelClass("bottle", "closed")]

    def test_state_is_propagated(self):
        res = postprocess(channel_first((0.5, 0.5, 0.4, 0.4, 0.10, 0.85, 0.0)), self.CLASSES, 320, IDENTITY)
        self.assertEqual(1, len(res))
        self.assertEqual(("bucket", "water"), (res[0].obj, res[0].state))

    def test_states_of_same_object_are_merged_by_nms(self):
        res = postprocess(
            channel_first((0.5, 0.5, 0.4, 0.4, 0.60, 0.10, 0.0), (0.5, 0.5, 0.4, 0.4, 0.10, 0.80, 0.0)),
            self.CLASSES, 320, IDENTITY,
        )
        self.assertEqual(1, len(res))
        self.assertEqual("water", res[0].state)

    def test_different_objects_in_same_place_are_kept(self):
        res = postprocess(
            channel_first((0.5, 0.5, 0.4, 0.4, 0.10, 0.80, 0.0), (0.5, 0.5, 0.4, 0.4, 0.0, 0.0, 0.90)),
            self.CLASSES, 320, IDENTITY,
        )
        self.assertEqual(2, len(res))
        self.assertEqual("bottle", res[0].obj)


if __name__ == "__main__":
    unittest.main()
