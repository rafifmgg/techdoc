#!/usr/bin/env python
"""
Word to Markdown Converter
Converts .docx files to .md (Markdown) format with proper table support
"""

import mammoth
import sys
import re
from pathlib import Path
from bs4 import BeautifulSoup


def html_table_to_markdown(soup):
    """Convert HTML tables to Markdown table format."""
    for table in soup.find_all('table'):
        rows = table.find_all('tr')
        if not rows:
            continue

        markdown_lines = []
        header_done = False

        for row in rows:
            cells = row.find_all(['th', 'td'])
            if not cells:
                continue

            cell_contents = []
            for cell in cells:
                text = cell.get_text(separator=' ', strip=True)
                text = re.sub(r'\s+', ' ', text)
                text = text.replace('|', r'\|')
                cell_contents.append(text)

            markdown_lines.append('| ' + ' | '.join(cell_contents) + ' |')

            if not header_done:
                separator = '| ' + ' | '.join(['---'] * len(cells)) + ' |'
                markdown_lines.append(separator)
                header_done = True

        markdown_table = '\n'.join(markdown_lines)
        table.replace_with(BeautifulSoup('\n\n' + markdown_table + '\n\n', 'html.parser'))

    return soup


def html_to_markdown(html_content: str) -> str:
    """Convert HTML to Markdown with proper table support."""
    soup = BeautifulSoup(html_content, 'html.parser')
    soup = html_table_to_markdown(soup)
    result = str(soup)

    result = re.sub(r'<h1[^>]*>(.*?)</h1>', r'\n# \1\n', result, flags=re.DOTALL)
    result = re.sub(r'<h2[^>]*>(.*?)</h2>', r'\n## \1\n', result, flags=re.DOTALL)
    result = re.sub(r'<h3[^>]*>(.*?)</h3>', r'\n### \1\n', result, flags=re.DOTALL)
    result = re.sub(r'<h4[^>]*>(.*?)</h4>', r'\n#### \1\n', result, flags=re.DOTALL)
    result = re.sub(r'<h5[^>]*>(.*?)</h5>', r'\n##### \1\n', result, flags=re.DOTALL)
    result = re.sub(r'<h6[^>]*>(.*?)</h6>', r'\n###### \1\n', result, flags=re.DOTALL)
    result = re.sub(r'<strong>(.*?)</strong>', r'**\1**', result, flags=re.DOTALL)
    result = re.sub(r'<b>(.*?)</b>', r'**\1**', result, flags=re.DOTALL)
    result = re.sub(r'<em>(.*?)</em>', r'*\1*', result, flags=re.DOTALL)
    result = re.sub(r'<i>(.*?)</i>', r'*\1*', result, flags=re.DOTALL)
    result = re.sub(r'<code>(.*?)</code>', r'`\1`', result, flags=re.DOTALL)
    result = re.sub(r'<a[^>]*href="([^"]*)"[^>]*>(.*?)</a>', r'[\2](\1)', result, flags=re.DOTALL)
    result = re.sub(r'<img[^>]*src="([^"]*)"[^>]*alt="([^"]*)"[^>]*/>', r'![\2](\1)', result, flags=re.DOTALL)
    result = re.sub(r'<img[^>]*src="([^"]*)"[^>]*/>', r'![](\1)', result, flags=re.DOTALL)
    result = re.sub(r'<li[^>]*>(.*?)</li>', r'\n- \1', result, flags=re.DOTALL)
    result = re.sub(r'<br\s*/?>', '\n', result)
    result = re.sub(r'<p[^>]*>(.*?)</p>', r'\n\1\n', result, flags=re.DOTALL)
    result = re.sub(r'<[^>]+>', '', result)
    result = re.sub(r'\n{3,}', '\n\n', result)
    result = re.sub(r' +', ' ', result)

    return result.strip()


def convert_word_to_markdown(input_path: str, output_path: str = None) -> str:
    """Convert a Word document (.docx) to Markdown format."""
    input_path = Path(input_path)

    if not input_path.exists():
        raise FileNotFoundError(f'File not found: {input_path}')

    if input_path.suffix.lower() != '.docx':
        raise ValueError(f'File must be a .docx file, got: {input_path.suffix}')

    if output_path is None:
        output_path = input_path.with_suffix('.md')
    else:
        output_path = Path(output_path)

    with open(input_path, 'rb') as docx_file:
        result = mammoth.convert_to_html(docx_file)
        html_content = result.value

        if result.messages:
            print('Warnings during conversion:')
            for message in result.messages:
                print(f'  - {message}')

    markdown_content = html_to_markdown(html_content)

    with open(output_path, 'w', encoding='utf-8') as md_file:
        md_file.write(markdown_content)

    return str(output_path)


def convert_directory(input_dir: str, output_dir: str = None) -> list:
    """Convert all Word documents in a directory to Markdown."""
    input_dir = Path(input_dir)
    output_dir = Path(output_dir) if output_dir else input_dir

    if not input_dir.is_dir():
        raise NotADirectoryError(f'Not a directory: {input_dir}')

    output_dir.mkdir(parents=True, exist_ok=True)
    converted_files = []
    docx_files = list(input_dir.glob('*.docx'))

    if not docx_files:
        print(f'No .docx files found in {input_dir}')
        return converted_files

    for docx_file in docx_files:
        output_path = output_dir / docx_file.with_suffix('.md').name
        try:
            result = convert_word_to_markdown(str(docx_file), str(output_path))
            converted_files.append(result)
            print(f'Converted: {docx_file.name} -> {output_path.name}')
        except Exception as e:
            print(f'Error converting {docx_file.name}: {e}')

    return converted_files


def main():
    if len(sys.argv) < 2:
        print('Word to Markdown Converter')
        print('-' * 40)
        print('Usage:')
        print('  Single file:  python word_to_md.py <input.docx> [output.md]')
        print('  Directory:    python word_to_md.py <input_dir> [output_dir] --dir')
        print()
        print('Examples:')
        print('  python word_to_md.py document.docx')
        print('  python word_to_md.py document.docx output.md')
        print('  python word_to_md.py ./docs --dir')
        print('  python word_to_md.py ./docs ./markdown_output --dir')
        sys.exit(1)

    input_path = sys.argv[1]
    output_path = sys.argv[2] if len(sys.argv) > 2 and sys.argv[2] != '--dir' else None
    is_directory = '--dir' in sys.argv

    try:
        if is_directory:
            results = convert_directory(input_path, output_path)
            print(f'\nConverted {len(results)} file(s)')
        else:
            result = convert_word_to_markdown(input_path, output_path)
            print(f'Successfully converted to: {result}')
    except Exception as e:
        print(f'Error: {e}')
        sys.exit(1)


if __name__ == '__main__':
    main()
