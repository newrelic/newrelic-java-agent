# Github Pages Utility Scripts

This directory contains command line scripts that read and write html, for the purposes of creating static html files for GH Pages.

## `writeIndexPage.py`

This script writes a generic `index.html` file for a directory.
The `index.html` file lists and links to the contents of the directory.

- **Argument**: `path/to/directory`
- **Returns**: void, creates an index page at `path/to/directory/index.html`

#### Requirements

- Any version of Python.

#### Usage

Make sure you are on the gh-pages branch of this repository.

To index `path/to/directory`, move to the root directory of the repository and run:

```
python ghpages_utils/writeIndexPage.py path/to/directory
```

## `writeTestsFailuresPage.py`

This script is written specifically for the build artifacts of this repository's unit tests. When our unit tests run for `module-name`, they generate summary index files at `module-name/build/reports/tests/test/index.html`. This script checks these files for test failures, and writes an .html file summarizing and linking to the results.

- **Argument**: `path/to/test/results`
- **Returns**: void, creates an .html summary page at `path/to/test/results/index.html`

#### Requirements

- Python 2 or higher
- `beautifulsoup`, install by running `pip install beautifulsoup4`

#### Usage

Make sure you are on the gh-pages branch of this repository.

Given a `path/to/test/results`, this script expects the file structure:

```
path/to/test/results
    module1-name
        build
            reports
                tests
                    test
                        ...
                        index.html
                        ...
    module2-name
        build
            reports
                tests
                    test
                        ...
                        index.html
                        ...
    ...
```
From the root of this repository, run:

```
python ghpages_utils/writeTestsFailuresPage.py path/to/test/results
```