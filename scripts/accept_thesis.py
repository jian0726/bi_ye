# -*- coding: utf-8 -*-
"""正文 docx Word COM 验收：
1) 就地更新 TOC 域
2) 统计页数/字数
3) 导出 PDF 供 fitz 抽样检查
"""
import os
import pythoncom
import win32com.client

DOCX = r'D:\quanbudaima\bi_ye_she_ji\docs\03-正文\毕业设计正文-简柚个人博客系统的设计与实现.docx'
PDF = r'D:\quanbudaima\bi_ye_she_ji\shots\_chk-thesis-final.pdf'

pythoncom.CoInitialize()
word = win32com.client.gencache.EnsureDispatch('Word.Application')
word.Visible = False
word.DisplayAlerts = 0
try:
    doc = word.Documents.Open(DOCX, False, False)
    # 更新目录域
    try:
        doc.TablesOfContents(1).Update()
        toc = 'TOC updated'
    except Exception as e:
        toc = 'TOC skip: %s' % e
    doc.Repaginate()
    pages = doc.ComputeStatistics(2)        # wdStatisticPages
    words = doc.ComputeStatistics(0)        # wdStatisticWords
    chars = doc.ComputeStatistics(3)        # wdStatisticCharacters
    doc.Save()
    doc.ExportAsFixedFormat(PDF, 17)        # wdExportFormatPDF
    doc.Close(False)
    print('pages=%d words=%d chars=%d' % (pages, words, chars))
    print(toc)
    print('pdf=%s %dB' % (PDF, os.path.getsize(PDF)))
finally:
    word.Quit()
    pythoncom.CoUninitialize()
