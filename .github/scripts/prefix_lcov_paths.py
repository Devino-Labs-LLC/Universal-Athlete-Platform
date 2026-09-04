#!/usr/bin/env python3
"""Rewrite LCOV SF: paths so Sonar (repository root) can match sources."""

from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]

REPORTS = (
    (REPO_ROOT / "apps/web/coverage/lcov.info", "apps/web"),
    (REPO_ROOT / "apps/mobile/coverage/lcov.info", "apps/mobile"),
)


def rewrite_source_path(raw: str, package_prefix: str) -> str:
    path = raw.strip()
    if not path:
        return path
    candidate = Path(path)
    if candidate.is_absolute():
        try:
            return candidate.resolve().relative_to(REPO_ROOT).as_posix()
        except ValueError:
            return path.replace("\\", "/")
    posix = path.replace("\\", "/")
    if posix.startswith(package_prefix + "/"):
        return posix
    return f"{package_prefix}/{posix.lstrip('/')}"


def rewrite_report(report: Path, package_prefix: str) -> None:
    if not report.is_file():
        raise SystemExit(f"Missing LCOV report: {report}")
    rewritten = []
    for line in report.read_text(encoding="utf-8").splitlines(keepends=True):
        if line.startswith("SF:"):
            suffix = "\n" if line.endswith("\n") else ""
            body = line[3:].rstrip("\r\n")
            rewritten.append(f"SF:{rewrite_source_path(body, package_prefix)}{suffix}")
        else:
            rewritten.append(line)
    report.write_text("".join(rewritten), encoding="utf-8")


def main() -> None:
    for report, prefix in REPORTS:
        rewrite_report(report, prefix)
        print(f"Rewrote {report.relative_to(REPO_ROOT)} for {prefix}")


if __name__ == "__main__":
    main()
