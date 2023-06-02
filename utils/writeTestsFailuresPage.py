import os
import argparse
from bs4 import BeautifulSoup

#Write an HTML file listing and forwarding to the failing test suites in a test folder.
#This script is specifically for this repository's unit test build artifacts.
#The expected index files are at module-name/build/reports/tests/test/index.html

def writeTestsFailuresPage(root_path):
    fail_count = 0
    failing_tests = ""
    passing_tests = ""
    for folder in os.listdir(root_path):
        summary_path = os.path.join(root_path, folder, 'build', 'reports', 'tests', 'test', 'index.html')
        if os.path.exists(summary_path):
            relative_path = os.path.relpath(summary_path, root_path)
            test_link = f"<li><a href='{relative_path}'>{relative_path}</a></li>"

            #Check the target HTML file for its reported number of failures.
            with open(summary_path, 'r') as html_file:
                soup = BeautifulSoup(html_file, 'html.parser')
                failures_counter = soup.find(id='failures').find(class_='counter')
                if failures_counter and ('0'!=failures_counter.get_text().strip()):
                    fail_count += 1
                    failing_tests += test_link
                else:
                    passing_tests += test_link

    html_string = f'''
      <!DOCTYPE html>
      <html>
      <head>
      <title>Unit Test Results</title>
      <style>
        #failing li {{
            color: red
        }}
        #passing li {{
            color: green
        }}
      </style>
      </head>
      <body>
      <h1>Unit Test Results</h1>
      <div id = 'failing'><h2>Failing Test Suites</h2>
          <ul> {failing_tests} </ul>
      </div>
      <div id='passing'> <h2>Passing Test Suites</h2>
          <ul> {passing_tests} </ul>
      </div>
      </body>
      </html>
      '''
    index_path = os.path.join(root_path, 'index.html')
    with open(index_path, 'w') as index_file:
        index_file.write(html_string)
    print(f'Test reports published to {root_path} with {fail_count} failing suites.')

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Description of your script')
    parser.add_argument('root', help='The root path of a folder containing test reports.')
    args = parser.parse_args()
    writeTestsFailuresPage(args.root)