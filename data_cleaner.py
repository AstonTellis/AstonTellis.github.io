# =============================================================================
# CSV Data Cleaner & Analyser — by Aston Tellis
# Portfolio Project · Python 3 · pandas
# GitHub: github.com/AstonTellis
# =============================================================================
# USAGE:
#   python data_cleaner.py your_file.csv
#
# REQUIREMENTS:
#   pip install pandas
#
# WHAT IT DOES:
#   - Detects missing values per column
#   - Identifies duplicate rows
#   - Calculates statistics for numeric columns
#   - Detects data type inconsistencies
#   - Outputs a formatted report as a .txt file
#
# SAFE TO RUN:
#   This script only reads the CSV you specify. It does not connect to the
#   internet, access other files, or collect any data.
# =============================================================================

import pandas as pd
import sys
import os
from datetime import datetime


def load_csv(path: str) -> pd.DataFrame:
    """Load a CSV file with UTF-8 encoding, fallback to latin-1."""
    try:
        return pd.read_csv(path, encoding='utf-8')
    except UnicodeDecodeError:
        print("  Note: UTF-8 failed, using latin-1 encoding.")
        return pd.read_csv(path, encoding='latin-1')
    except FileNotFoundError:
        print(f"Error: File not found — '{path}'")
        sys.exit(1)
    except Exception as e:
        print(f"Error reading file: {e}")
        sys.exit(1)


def format_number(n: float) -> str:
    """Format large numbers readably."""
    if abs(n) >= 1_000_000:
        return f"{n:,.2f}"
    return f"{n:,.4f}".rstrip('0').rstrip('.')


def analyse(df: pd.DataFrame, filename: str) -> str:
    """Run full analysis on a DataFrame and return a report string."""
    lines = []
    sep   = "=" * 60

    # Header
    lines.append(sep)
    lines.append("  CSV DATA ANALYSIS REPORT")
    lines.append(f"  Built by Aston Tellis — github.com/AstonTellis")
    lines.append(sep)
    lines.append(f"  File      : {os.path.basename(filename)}")
    lines.append(f"  Generated : {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    lines.append(sep)

    # ── Overview ──────────────────────────────────────────────
    lines.append("\n📋 OVERVIEW")
    lines.append("-" * 40)
    lines.append(f"  Rows             : {df.shape[0]:,}")
    lines.append(f"  Columns          : {df.shape[1]}")
    lines.append(f"  Total cells      : {df.shape[0] * df.shape[1]:,}")

    duplicate_count = df.duplicated().sum()
    lines.append(f"  Duplicate rows   : {duplicate_count:,}")
    if duplicate_count > 0:
        dup_pct = duplicate_count / len(df) * 100
        lines.append(f"  → {dup_pct:.1f}% of rows are exact duplicates")
        lines.append(f"  → Run df.drop_duplicates() to clean")

    # ── Column Overview ────────────────────────────────────────
    lines.append("\n📊 COLUMN TYPES")
    lines.append("-" * 40)
    for col in df.columns:
        dtype = str(df[col].dtype)
        unique = df[col].nunique()
        lines.append(f"  {col:<30} {dtype:<15} {unique:,} unique values")

    # ── Missing Values ─────────────────────────────────────────
    lines.append("\n⚠  MISSING VALUES")
    lines.append("-" * 40)
    missing_any = False
    for col in df.columns:
        missing = df[col].isna().sum()
        if missing > 0:
            missing_any = True
            pct = missing / len(df) * 100
            severity = "HIGH" if pct > 30 else "MEDIUM" if pct > 10 else "LOW"
            lines.append(f"  {col:<30} {missing:>6,} missing  ({pct:5.1f}%)  [{severity}]")
    if not missing_any:
        lines.append("  ✓ No missing values detected — clean dataset!")

    # ── Numeric Column Stats ───────────────────────────────────
    numeric_cols = df.select_dtypes(include=['number']).columns.tolist()
    if numeric_cols:
        lines.append("\n📈 NUMERIC COLUMN STATISTICS")
        lines.append("-" * 40)
        for col in numeric_cols:
            col_data = df[col].dropna()
            if col_data.empty:
                continue
            lines.append(f"\n  Column: {col}")
            lines.append(f"    Count   : {len(col_data):,}")
            lines.append(f"    Mean    : {format_number(col_data.mean())}")
            lines.append(f"    Median  : {format_number(col_data.median())}")
            lines.append(f"    Std Dev : {format_number(col_data.std())}")
            lines.append(f"    Min     : {format_number(col_data.min())}")
            lines.append(f"    Max     : {format_number(col_data.max())}")
            lines.append(f"    25th %  : {format_number(col_data.quantile(0.25))}")
            lines.append(f"    75th %  : {format_number(col_data.quantile(0.75))}")

            # Outlier detection (IQR method)
            q1  = col_data.quantile(0.25)
            q3  = col_data.quantile(0.75)
            iqr = q3 - q1
            outliers = ((col_data < q1 - 1.5 * iqr) | (col_data > q3 + 1.5 * iqr)).sum()
            if outliers > 0:
                lines.append(f"    Outliers: {outliers:,} detected (IQR method)")

    # ── Text Column Stats ──────────────────────────────────────
    text_cols = df.select_dtypes(include=['object']).columns.tolist()
    if text_cols:
        lines.append("\n🔤 TEXT COLUMN ANALYSIS")
        lines.append("-" * 40)
        for col in text_cols:
            col_data = df[col].dropna()
            if col_data.empty:
                continue
            unique_count  = col_data.nunique()
            cardinality   = unique_count / len(col_data) * 100
            most_common   = col_data.value_counts().index[0] if len(col_data) > 0 else "N/A"
            most_common_n = col_data.value_counts().iloc[0] if len(col_data) > 0 else 0

            lines.append(f"\n  Column: {col}")
            lines.append(f"    Unique values : {unique_count:,} ({cardinality:.1f}% cardinality)")
            lines.append(f"    Most common   : '{most_common}' ({most_common_n:,} times)")
            if cardinality < 5:
                lines.append(f"    → Low cardinality — could be a categorical/enum column")
            elif cardinality > 90:
                lines.append(f"    → High cardinality — likely a unique identifier or free text")

    # ── Recommendations ────────────────────────────────────────
    lines.append("\n💡 RECOMMENDATIONS")
    lines.append("-" * 40)

    recs = []
    if duplicate_count > 0:
        recs.append(f"Remove {duplicate_count:,} duplicate rows with df.drop_duplicates()")

    for col in df.columns:
        missing = df[col].isna().sum()
        pct = missing / len(df) * 100 if len(df) > 0 else 0
        if pct > 50:
            recs.append(f"Consider dropping column '{col}' — {pct:.0f}% missing")
        elif 0 < pct <= 50:
            recs.append(f"Fill missing values in '{col}' (df['{col}'].fillna(...))")

    if not recs:
        recs.append("Dataset looks clean — no major issues detected.")

    for i, rec in enumerate(recs, 1):
        lines.append(f"  {i}. {rec}")

    lines.append(f"\n{sep}")
    lines.append(f"  Report complete · Aston Tellis · astontellis.github.io")
    lines.append(f"{sep}\n")

    return "\n".join(lines)


def main():
    print("\n" + "=" * 60)
    print("  CSV Data Cleaner & Analyser · by Aston Tellis")
    print("=" * 60)

    if len(sys.argv) < 2:
        print("\nUsage: python data_cleaner.py <your_file.csv>")
        print("Example: python data_cleaner.py sales_data.csv\n")
        sys.exit(1)

    filepath = sys.argv[1]

    print(f"\n→ Loading: {filepath}")
    df = load_csv(filepath)
    print(f"✓ Loaded {df.shape[0]:,} rows × {df.shape[1]} columns")

    print("→ Running analysis...")
    report = analyse(df, filepath)

    # Print to console
    print(report)

    # Save to file
    base     = os.path.splitext(filepath)[0]
    out_path = f"{base}_report.txt"
    with open(out_path, 'w', encoding='utf-8') as f:
        f.write(report)

    print(f"✓ Report saved to: {out_path}")


if __name__ == "__main__":
    main()
