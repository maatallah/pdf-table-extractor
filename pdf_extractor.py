#!/usr/bin/env python3
"""
PDF Table Extractor - Python Edition using pdfplumber
Extracts tables from PDF files with support for markers, filtering, and multiple export formats.
"""

import pdfplumber
import pandas as pd
import argparse
import yaml
import sys
from pathlib import Path


class PDFTableExtractor:
    def __init__(self, config_path=None):
        self.config = None
        if config_path:
            with open(config_path, 'r') as f:
                self.config = yaml.safe_load(f)
    
    def extract_tables(self, pdf_path, pages='all'):
        """Extract tables from PDF using pdfplumber"""
        all_tables = []
        
        with pdfplumber.open(pdf_path) as pdf:
            page_nums = range(len(pdf.pages)) if pages == 'all' else self.parse_pages(pages)
            
            for page_num in page_nums:
                if page_num >= len(pdf.pages):
                    continue
                
                page = pdf.pages[page_num]
                tables = page.extract_tables()
                
                for table in tables:
                    if table:
                        all_tables.append(table)
        
        return all_tables
    
    def parse_pages(self, pages_str):
        """Parse page range string like '1,2,3' or '1-3'"""
        pages = []
        for part in pages_str.split(','):
            if '-' in part:
                start, end = map(int, part.split('-'))
                pages.extend(range(start-1, end))  # 0-indexed
            else:
                pages.append(int(part) - 1)  # 0-indexed
        return pages
    
    def filter_by_markers(self, df, start_marker=None, end_marker=None):
        """Filter dataframe rows based on start and end markers"""
        if df.empty:
            return df
        
        start_idx = 0
        end_idx = len(df)
        
        # Find start marker
        if start_marker:
            for idx, row in df.iterrows():
                row_text = ' '.join([str(cell) for cell in row if pd.notna(cell)])
                if start_marker in row_text:
                    start_idx = idx
                    break
        
        # Find end marker
        if end_marker:
            for idx, row in df.iterrows():
                if idx >= start_idx:
                    row_text = ' '.join([str(cell) for cell in row if pd.notna(cell)])
                    if end_marker in row_text:
                        end_idx = idx
                        break
        
        return df.iloc[start_idx:end_idx]
    
    def clean_data(self, df):
        """Clean extracted data - remove footers, fix decimals, etc."""
        # Replace comma decimal separators with periods
        df = df.applymap(lambda x: str(x).replace(',', '.') if pd.notna(x) else x)
        
        # Filter out footer patterns
        footer_patterns = [
            'JAKO AG', 'Managing Directors', 'Chairman of the Board',
            'Tax No.', 'VAT No.', 'IBAN:', 'BIC:', 'Tel.', 'Fax', 'www.'
        ]
        
        mask = df.apply(lambda row: not any(
            pattern in str(cell) for cell in row for pattern in footer_patterns
        ), axis=1)
        df = df[mask]
        
        # Filter out page headers
        mask = df.apply(lambda row: not any(
            'Page' in str(cell) and 'of' in str(cell) for cell in row
        ), axis=1)
        df = df[mask]
        
        # Filter out "Unit of" row
        mask = df.apply(lambda row: not any(
            str(cell).strip() == 'Unit of' for cell in row
        ), axis=1)
        df = df[mask]
        
        return df
    
    def merge_multiline_cells(self, df):
        """Merge continuation rows (like BLUE BLACK, SENIOREN WEEL) into main rows"""
        if df.empty or len(df.columns) < 10:
            return df
        
        merged_rows = []
        current_row = None
        
        for idx, row in df.iterrows():
            # Check if this is a main row (has article number in first column)
            first_cell = str(row.iloc[0]) if pd.notna(row.iloc[0]) else ''
            
            if first_cell.strip() and not first_cell.strip().startswith(('BLUE', 'SENIOREN', '672')):
                # Save previous row if exists
                if current_row is not None:
                    merged_rows.append(current_row)
                # Start new row
                current_row = row.copy()
            elif current_row is not None:
                # This is a continuation row - merge into colour column (index 4)
                for i, cell in enumerate(row):
                    if pd.notna(cell) and str(cell).strip():
                        # Append to colour column
                        if i <= 4 and pd.notna(current_row.iloc[4]):
                            current_row.iloc[4] = str(current_row.iloc[4]) + ' ' + str(cell)
        
        # Add last row
        if current_row is not None:
            merged_rows.append(current_row)
        
        return pd.DataFrame(merged_rows)
    
    def export(self, df, output_path, format='csv'):
        """Export dataframe to specified format"""
        output_path = Path(output_path)
        
        if format == 'csv':
            df.to_csv(output_path, index=False, header=True)
        elif format == 'json':
            df.to_json(output_path, orient='records', indent=2)
        elif format == 'xml':
            df.to_xml(output_path, index=False)
        elif format == 'xlsx':
            df.to_excel(output_path, index=False, engine='openpyxl')
        else:
            raise ValueError(f"Unsupported format: {format}")
        
        print(f"Extraction complete. Output saved to {output_path}")


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
    
    # Extract tables
    print(f"Input: {args.input}")
    print(f"Output: {args.output}")
    print(f"Format: {args.format}")
    print(f"Pages: {args.pages}")
    
    tables = extractor.extract_tables(args.input, pages=args.pages)
    
    if len(tables) == 0:
        print("No tables found in PDF")
        sys.exit(1)
    
    # Convert tables to dataframes and combine
    all_dfs = []
    for table in tables:
        df = pd.DataFrame(table)
        all_dfs.append(df)
    
    combined_df = pd.concat(all_dfs, ignore_index=True)
    
    # Apply feedback configuration if provided
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
    
    # Filter by markers
    if start_marker or end_marker:
        combined_df = extractor.filter_by_markers(combined_df, start_marker, end_marker)
    
    # Clean data
    combined_df = extractor.clean_data(combined_df)
    
    # Merge multi-line cells
    combined_df = extractor.merge_multiline_cells(combined_df)
    
    # Set proper column headers
    if not combined_df.empty and len(combined_df.columns) >= 10:
        combined_df.columns = ['No.', 'Description', 'SR', 'Size', 'Colour', 
                                'Quantity', 'Unit of Measure', 'Price', 'Disc. %', 'Amount']
    
    # Export
    extractor.export(combined_df, args.output, args.format)


if __name__ == '__main__':
    main()
