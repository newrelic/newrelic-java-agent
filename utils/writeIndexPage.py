import os
import argparse

def writeIndexPage(path):
    dirName = os.path.basename(path)

    contents_list = "<ul>"
    for item in os.listdir(path):
        contents_list += f'<li class="file"><a href="{item}">{item}</a></li>'
    contents_list += "</ul>"

    html_string = f'''
    <!DOCTYPE html>
    <html>
    <head>
    <title>{dirName}</title>
    </head>
    <body>
    <h1>{dirName}</h1>
    {contents_list}
    </body>
    </html>
    '''

    indexPath = os.path.join(path, 'index.html')
    with open(indexPath, 'w') as file:
        file.write(html_string)

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Write an index.html file for a directory.')
    parser.add_argument('root', help='The root path of a folder to index.')
    args = parser.parse_args()
    writeIndexPage(args.root)