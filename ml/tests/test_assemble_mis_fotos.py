import unittest

from ml.assemble_mis_fotos import group_bursts, split_groups, stamp_of


def _name(hhmmss: str) -> str:
    return f"vacio_IMG_20261003_{hhmmss}.jpg"


class AssembleMisFotosTest(unittest.TestCase):
    def test_stamp(self):
        self.assertEqual(stamp_of(_name("102501")).second, 1)
        with self.assertRaises(ValueError):
            stamp_of("sin_fecha.jpg")

    def test_bursts_split_on_gap(self):
        names = [_name(t) for t in ("102501", "102504", "102520", "102700", "102703")]
        groups = group_bursts(names, gap_s=30)
        self.assertEqual([len(g) for g in groups], [3, 2])

    def test_burst_never_split_across_splits(self):
        groups = [[f"g{g}_{i}" for i in range(n)] for g, n in enumerate((20, 8, 8, 5, 4))]
        split_of = split_groups(groups)
        for g in groups:
            self.assertEqual(len({split_of[n] for n in g}), 1)
        self.assertEqual({"train", "val", "test"}, set(split_of.values()))

    def test_too_few_groups_all_train(self):
        split_of = split_groups([["a", "b"], ["c"]])
        self.assertEqual(set(split_of.values()), {"train"})

    def test_deterministic(self):
        groups = [[f"g{g}_{i}" for i in range(n)] for g, n in enumerate((10, 7, 6, 5))]
        self.assertEqual(split_groups(groups), split_groups(groups))


if __name__ == "__main__":
    unittest.main()
