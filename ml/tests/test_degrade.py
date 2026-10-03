import unittest

import numpy as np
from PIL import Image

from ml.degrade import degrade


def _img() -> Image.Image:
    rng = np.random.default_rng(1)
    return Image.fromarray(rng.integers(0, 255, (96, 128, 3), dtype=np.uint8))


class DegradeTest(unittest.TestCase):
    def test_keeps_size_and_mode(self):
        out = degrade(_img(), "a.jpg")
        self.assertEqual(out.size, (128, 96))
        self.assertEqual(out.mode, "RGB")

    def test_deterministic_per_seed(self):
        a, b = degrade(_img(), "a.jpg"), degrade(_img(), "a.jpg")
        self.assertTrue(np.array_equal(np.asarray(a), np.asarray(b)))

    def test_changes_the_image(self):
        for seed in ("a.jpg", "b.jpg", "c.jpg", "d.jpg"):
            self.assertFalse(np.array_equal(np.asarray(degrade(_img(), seed)), np.asarray(_img())))

    def test_different_seeds_differ(self):
        outs = {degrade(_img(), s).tobytes() for s in ("a", "b", "c", "d", "e")}
        self.assertGreater(len(outs), 1)


if __name__ == "__main__":
    unittest.main()
