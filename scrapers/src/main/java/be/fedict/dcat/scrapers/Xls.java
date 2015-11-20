/*
 * Copyright (c) 2015, Bart Hanssens <bart.hanssens@fedict.be>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package be.fedict.dcat.scrapers;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract scraper for Excel files.
 * 
 * @author Bart Hanssens <bart.hanssens@fedict.be>
 */
public abstract class Xls extends Scraper {
    private final Logger logger = LoggerFactory.getLogger(Xls.class);
    
    /**
     * Get the column names as a list
     * 
     * @param sheet
     * @return list of column names
     */
    public List<String> getColumnNames(Sheet sheet) {
        ArrayList<String> headers = new ArrayList<>();
        
        Row header = sheet.getRow(sheet.getFirstRowNum());
        Iterator<Cell> cells = header.cellIterator();
        while (cells.hasNext()) {
            Cell cell = cells.next();
            logger.info(cell.getStringCellValue());
            headers.add(cell.getStringCellValue());
        }
        return headers;
    }
    
    /**
     * Return the number of rows, minus the header
     * 
     * @param sheet
     * @return number of rows - 1 
     */
    public int getRows(Sheet sheet) {
        return sheet.getLastRowNum() - sheet.getFirstRowNum();
    }
    
    /**
     * Scrape rows in sheet
     * 
     * @return number of datasets
     */
    public abstract int scrapeRows(Workbook wb);
    
    @Override
    public void scrape() throws IOException {
        logger.info("Start scraping");

        Workbook wb = null;
        
        try {
            File f = new File(getBase().toURI());
            wb = WorkbookFactory.create(f);
        } catch (URISyntaxException|InvalidFormatException ex) {
            logger.error("Could not load file {}", getBase());
            throw new IOException(ex);
        }
        int rows = scrapeRows(wb);
        logger.info("Found {} rows", rows);

        logger.info("Done scraping");
    }

    /**
     * Constructor
     * 
     * @param caching 
     * @param storage SDB file to be used as triple store backend
     * @param base path to file
     */
    public Xls(File caching, File storage, URL base) {
        super(caching, storage, base);
    }
}
