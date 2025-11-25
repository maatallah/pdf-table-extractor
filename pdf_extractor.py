#!/usr/bin/env python3
"""
PDF Table Extractor - Custom text-based parsing
Extracts tables from PDF files using text extraction and custom parsing logic.
"""

import pdfplumber
import pandas as pd
import argparse
import yaml
import re
from pathlib import Path


class PDFTableExtractor:
    def __init__(self, config_path=None):
        self.config = None
        if config_path:
            with open(config_path, 'r') as f:
                self.config = yaml.safe_load(f)
    
    def extract_text_with_layout(self, pdf_path, pages='all'):
        """Extract text from PDF preserving layout"""
        all_text = []
        
        with pdfplumber.open(pdf_path) as pdf:
            page_nums = range(len(pdf.pages)) if pages == 'all' else self.parse_pages(pages)
            
            for page_num in page_nums:
                if page_num >= len(pdf.pages):
                    continue
                
                page = pdf.pages[page_num]
                text = page.extract_text()
                if text:
                    all_text.append(text)
        
        return '\n'.join(all_text)
    
    def parse_pages(self, pages_str):
        """Parse page range string like '1,2,3' or '1-3'"""
        pages = []
        for part in pages_str.split(','):
            if '-' in part:
                start, end = map(int, part.split('-'))
                pages.extend(range(start-1, end))
            else:
                pages.append(int(part) - 1)
        return pages
    
    def parse_table_from_text(self, text, start_marker=None, end_marker=None):
        """Parse table data from extracted text"""
        lines = text.split('\n')
        
        # Find start and end indices
        start_idx = 0
        end_idx = len(lines)
        
        if start_marker:
            for i, line in enumerate(lines):
                if start_marker in line:
                    start_idx = i + 1  # Skip the header line itself
                    break
        
        if end_marker:
            for i in range(start_idx, len(lines)):
                if end_marker in lines[i]:
                    end_idx = i
                    break
        
        # Extract relevant lines
        table_lines = lines[start_idx:end_idx]
        
        # Parse data rows
        rows = []
        current_row = None
        
        # More flexible pattern - matches article number followed by description
        # Example: B4390127 SMU SHIRT LM K1 164 672 VITESSE 22/ 15 Stück 11,57 173,55
        main_row_pattern = re.compile(
            r'^([A-Z0-9]+)\s+'  # Article No.
            r'(.*?)\s+'  # Description (non-greedy)
            r'([A-Z0-9]+)\s+'  # SR
            r'(\d+)\s+'  # Size
            r'(.+?)\s+'  # Colour start
            r'(\d+)\s+'  # Quantity
            r'Stück\s+'  # Unit
            r'([\d,]+)\s*'  # Price
            r'([\d,]*)\s*'  # Disc % (optional)
            r'([\d,]+)$'  # Amount (end of line)
        )
        
        for line in table_lines:
            line = line.strip()
            if not line:
                continue
            
            # Skip footer patterns
            if any(pattern in line for pattern in [
                'JAKO AG', 'Managing Directors', 'Chairman of the Board',
                'Tax No.', 'VAT No.', 'IBAN:', 'BIC:', 'Tel.', 'Fax', 'www.',
                'Page', 'Unit of', 'Measure Price'
            ]):
                continue
            
            # Check if this is a continuation line (colour description)
            if current_row and any(keyword in line for keyword in [
                'BLUE BLACK', 'SENIOREN WEEL', 'CITRO WEEL', 'RED WEEL', '672 VITESSE'
            ]):
                # Only add if not already in colour field
                if line.strip() not in current_row['Colour']:
                    current_row['Colour'] += ' ' + line.strip()
                continue
            
            # Try to match main row pattern
            match = main_row_pattern.match(line)
            if match:
                # Save previous row
                if current_row:
                    rows.append(current_row)
                
                # Create new row
                current_row = {
                    'No.': match.group(1),
                    'Description': match.group(2).strip(),
                    'SR': match.group(3),
                    'Size': match.group(4),
                    'Colour': match.group(5).strip(),
                    'Quantity': match.group(6),
                    'Unit of Measure': 'Stück',
                    'Price': match.group(7).replace(',', '.'),
                    'Disc. %': match.group(8).replace(',', '.') if match.group(8) else '',
                    'Amount': match.group(9).replace(',', '.')
                }
        
        # Add last row
        if current_row:
            rows.append(current_row)
        
        return rows
    
    def export(self, rows, output_path, format='csv'):
        """Export rows to specified format"""
        if not rows:
            print("No data to export")
            return
        
        df = pd.DataFrame(rows)
        output_path = Path(output_path)
        
        if format == 'csv':
            df.to_csv(output_path, index=False)
        elif format == 'json':
            df.to_json(output_path, orient='records', indent=2)
        elif format == 'xml':
            df.to_xml(output_path, index=False)
        elif format == 'xlsx':
            df.to_excel(output_path, index=False, engine='openpyxl')
        else:
            raise ValueError(f"Unsupported format: {format}")
        
        print(f"Extraction complete. Output saved to {output_path}")
        print(f"Extracted {len(rows)} rows")


def main():
    parser = argparse.ArgumentParser(description='Extract tables from PDF files')
    parser.add_argument('-i', '--input', required=True, help='Input PDF file')
    parser.add_argument('-o', '--output', required=True, help='Output file')
    parser.add_argument('-f', '--format', default='csv', choices=['csv', 'json', 'xml', 'xlsx'],
                        help='Output format')
    parser.add_argument('-p', '--pages', default='all', help='Pages to extract (e.g., "1,2,3" or "all")')
    parser.add_argument('-fb', '--feedback', help='Feedback YAML configuration file')
    
    args = parser.parse_args()
    
    # Initialize extractor
    extractor = PDFTableExtractor(args.feedback)
    
    print(f"Input: {args.input}")
    print(f"Output: {args.output}")
    print(f"Format: {args.format}")
    print(f"Pages: {args.pages}")
    
    # Extract text
    text = extractor.extract_text_with_layout(args.input, pages=args.pages)
    
    # Get markers from config
    start_marker = None
    end_marker = None
    
    if extractor.config and 'files' in extractor.config:
        pdf_name = Path(args.input).name
        for file_config in extractor.config['files']:
            if file_config.get('filename') == pdf_name:
                overrides = file_config.get('overrides', {})
                start_marker = overrides.get('startMarker')
                end_marker = overrides.get('endMarker')
                break
    
    # Parse table
    rows = extractor.parse_table_from_text(text, start_marker, end_marker)
    
    if not rows:
        print("No data extracted")
        return
    
    # Export
    extractor.export(rows, args.output, args.format)


if __name__ == '__main__':
    main()
