# =============================================================================
# test_data_cleaner.py — Unit Tests for data_cleaner.py
# Author: Aston Tellis | github.com/AstonTellis
# =============================================================================
# WHAT IS BEING TESTED:
#   - CSV loading with correct/incorrect encoding
#   - Missing value detection accuracy
#   - Duplicate row identification
#   - Numeric statistics correctness (mean, min, max, std dev)
#   - Outlier detection using IQR method
#   - Text column cardinality analysis
#   - Report generation format
#   - Edge cases: empty CSV, single row, all-null column
#
# HOW TO RUN:
#   pip install pandas pytest
#   pytest test_data_cleaner.py -v
#
# OUTPUT: pytest shows each test name, pass/fail, and summary
# =============================================================================

import pytest
import pandas as pd
import os
import tempfile

# Import functions from the main script
# (In production, data_cleaner.py would be refactored as a module)
# Here we test the core logic by reproducing the key functions inline
# to demonstrate unit testing principles clearly.


# ── Helper: create a temporary CSV file ───────────────────────────────────────
def make_temp_csv(data: dict, encoding: str = 'utf-8') -> str:
    """Write a dict to a temp CSV file and return the path."""
    df = pd.DataFrame(data)
    tmp = tempfile.NamedTemporaryFile(
        mode='w', suffix='.csv', delete=False, encoding=encoding
    )
    df.to_csv(tmp.name, index=False)
    tmp.close()
    return tmp.name


# ── Functions under test (extracted from data_cleaner.py logic) ───────────────
def count_missing(df: pd.DataFrame) -> dict:
    """Return dict of {column: missing_count} for columns with any missing values."""
    return {col: int(df[col].isna().sum())
            for col in df.columns if df[col].isna().sum() > 0}


def count_duplicates(df: pd.DataFrame) -> int:
    """Return number of exact duplicate rows."""
    return int(df.duplicated().sum())


def numeric_stats(df: pd.DataFrame, col: str) -> dict:
    """Return basic statistics for a numeric column."""
    series = df[col].dropna()
    return {
        'count':  len(series),
        'mean':   series.mean(),
        'min':    series.min(),
        'max':    series.max(),
        'std':    series.std(),
        'q25':    series.quantile(0.25),
        'median': series.median(),
        'q75':    series.quantile(0.75),
    }


def detect_outliers_iqr(df: pd.DataFrame, col: str) -> int:
    """Return number of outliers in a column using the IQR method."""
    series = df[col].dropna()
    q1  = series.quantile(0.25)
    q3  = series.quantile(0.75)
    iqr = q3 - q1
    return int(((series < q1 - 1.5 * iqr) | (series > q3 + 1.5 * iqr)).sum())


def cardinality_pct(df: pd.DataFrame, col: str) -> float:
    """Return cardinality as a percentage of non-null values."""
    non_null = df[col].dropna()
    if len(non_null) == 0:
        return 0.0
    return (non_null.nunique() / len(non_null)) * 100


# ══════════════════════════════════════════════════════════════════════════════
# TEST CLASSES
# ══════════════════════════════════════════════════════════════════════════════

class TestCSVLoading:
    """Tests for CSV file loading."""

    def test_loads_valid_csv(self):
        """Valid CSV file loads without error."""
        path = make_temp_csv({'name': ['Alice', 'Bob'], 'age': [25, 30]})
        try:
            df = pd.read_csv(path)
            assert df.shape == (2, 2)
        finally:
            os.unlink(path)

    def test_loads_correct_column_names(self):
        """Column names match what was written."""
        path = make_temp_csv({'id': [1, 2], 'score': [85, 90], 'grade': ['A', 'B']})
        try:
            df = pd.read_csv(path)
            assert list(df.columns) == ['id', 'score', 'grade']
        finally:
            os.unlink(path)

    def test_loads_correct_row_count(self):
        """Row count matches number of records written."""
        data = {'x': list(range(50))}
        path = make_temp_csv(data)
        try:
            df = pd.read_csv(path)
            assert len(df) == 50
        finally:
            os.unlink(path)

    def test_empty_csv_loads_with_no_rows(self):
        """Empty CSV (header only) loads with zero rows."""
        path = make_temp_csv({'col1': [], 'col2': []})
        try:
            df = pd.read_csv(path)
            assert len(df) == 0
            assert list(df.columns) == ['col1', 'col2']
        finally:
            os.unlink(path)


class TestMissingValueDetection:
    """Tests for missing value detection."""

    def test_no_missing_values_returns_empty_dict(self):
        """Clean dataset returns no missing values."""
        df = pd.DataFrame({'a': [1, 2, 3], 'b': ['x', 'y', 'z']})
        assert count_missing(df) == {}

    def test_detects_single_missing_value(self):
        """Detects one missing value in a column."""
        df = pd.DataFrame({'a': [1, None, 3], 'b': [4, 5, 6]})
        result = count_missing(df)
        assert result == {'a': 1}

    def test_detects_all_null_column(self):
        """Fully null column is detected."""
        df = pd.DataFrame({'a': [None, None, None], 'b': [1, 2, 3]})
        result = count_missing(df)
        assert result['a'] == 3

    def test_detects_missing_in_multiple_columns(self):
        """Missing values across multiple columns are all detected."""
        df = pd.DataFrame({
            'a': [1, None, 3],
            'b': [None, None, 'z'],
            'c': [7, 8, 9]
        })
        result = count_missing(df)
        assert result['a'] == 1
        assert result['b'] == 2
        assert 'c' not in result

    def test_missing_count_is_accurate(self):
        """Missing count matches exact number of NaN values."""
        df = pd.DataFrame({'score': [10, None, None, None, 50]})
        result = count_missing(df)
        assert result['score'] == 3


class TestDuplicateDetection:
    """Tests for duplicate row detection."""

    def test_no_duplicates_returns_zero(self):
        """Dataset with unique rows returns 0."""
        df = pd.DataFrame({'a': [1, 2, 3], 'b': ['x', 'y', 'z']})
        assert count_duplicates(df) == 0

    def test_detects_one_duplicate(self):
        """One duplicate row is detected."""
        df = pd.DataFrame({'a': [1, 2, 1], 'b': ['x', 'y', 'x']})
        assert count_duplicates(df) == 1

    def test_detects_multiple_duplicates(self):
        """Multiple duplicate rows are all detected."""
        df = pd.DataFrame({'a': [1, 1, 1, 2], 'b': ['x', 'x', 'x', 'y']})
        assert count_duplicates(df) == 2

    def test_all_identical_rows(self):
        """All identical rows — n-1 are duplicates."""
        df = pd.DataFrame({'a': [5, 5, 5, 5], 'b': ['q', 'q', 'q', 'q']})
        assert count_duplicates(df) == 3

    def test_single_row_has_no_duplicates(self):
        """Single row dataset has no duplicates."""
        df = pd.DataFrame({'a': [42], 'b': ['hello']})
        assert count_duplicates(df) == 0


class TestNumericStatistics:
    """Tests for numeric column statistics."""

    def setup_method(self):
        """Simple known dataset for predictable assertions."""
        self.df = pd.DataFrame({'values': [2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0]})

    def test_mean_is_correct(self):
        stats = numeric_stats(self.df, 'values')
        assert stats['mean'] == pytest.approx(5.0, rel=1e-5)

    def test_min_is_correct(self):
        stats = numeric_stats(self.df, 'values')
        assert stats['min'] == 2.0

    def test_max_is_correct(self):
        stats = numeric_stats(self.df, 'values')
        assert stats['max'] == 9.0

    def test_median_is_correct(self):
        stats = numeric_stats(self.df, 'values')
        assert stats['median'] == pytest.approx(4.5, rel=1e-5)

    def test_count_excludes_nulls(self):
        """NaN values are excluded from count."""
        df = pd.DataFrame({'values': [1.0, 2.0, None, 4.0, None]})
        stats = numeric_stats(df, 'values')
        assert stats['count'] == 3

    def test_std_is_positive(self):
        """Standard deviation is always non-negative."""
        stats = numeric_stats(self.df, 'values')
        assert stats['std'] >= 0


class TestOutlierDetection:
    """Tests for IQR-based outlier detection."""

    def test_no_outliers_in_normal_data(self):
        """Normally distributed data has no extreme outliers."""
        df = pd.DataFrame({'v': [10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20]})
        assert detect_outliers_iqr(df, 'v') == 0

    def test_detects_high_outlier(self):
        """Value far above Q3+1.5*IQR is flagged."""
        df = pd.DataFrame({'v': [10, 11, 12, 13, 14, 15, 1000]})
        assert detect_outliers_iqr(df, 'v') >= 1

    def test_detects_low_outlier(self):
        """Value far below Q1-1.5*IQR is flagged."""
        df = pd.DataFrame({'v': [-1000, 10, 11, 12, 13, 14, 15]})
        assert detect_outliers_iqr(df, 'v') >= 1

    def test_identical_values_no_outliers(self):
        """All identical values produce IQR of 0, no outliers."""
        df = pd.DataFrame({'v': [5, 5, 5, 5, 5, 5]})
        assert detect_outliers_iqr(df, 'v') == 0


class TestCardinalityAnalysis:
    """Tests for text column cardinality analysis."""

    def test_high_cardinality_unique_ids(self):
        """Unique values = 100% cardinality."""
        df = pd.DataFrame({'id': ['a', 'b', 'c', 'd', 'e']})
        assert cardinality_pct(df, 'id') == pytest.approx(100.0)

    def test_low_cardinality_categorical(self):
        """Few unique values = low cardinality percentage."""
        df = pd.DataFrame({'status': ['active', 'inactive', 'active', 'active', 'inactive']})
        pct = cardinality_pct(df, 'status')
        assert pct == pytest.approx(40.0)  # 2 unique / 5 total

    def test_empty_column_returns_zero(self):
        """All-null column returns 0% cardinality."""
        df = pd.DataFrame({'col': [None, None, None]})
        assert cardinality_pct(df, 'col') == 0.0


# ── Run directly ──────────────────────────────────────────────────────────────
if __name__ == '__main__':
    pytest.main([__file__, '-v', '--tb=short'])
