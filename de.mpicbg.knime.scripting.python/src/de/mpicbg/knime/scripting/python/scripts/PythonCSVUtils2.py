import pandas as pd


# Read a CSV file into an OrderedDict.  The first line of the file is assumed to be the column names.
# The second line must contain the column types, column types will be inferred first then
# the attempt to convert them follows
#
def read_csv(csv_filename):
	# read data with column headers, row ids and infer data types
	pdf = pd.read_csv(csv_filename, skiprows = [1], sep = ',', header = 0, index_col = 0)
	# extract expected data types
	typesdf = pd.read_csv(testfile, sep=',', nrows = 1)
	typesdf = typesdf.drop('Row ID', axis = 1)
	types = dict()	
	for k in typesdf:
    	types[k] = typesdf.iloc[0][k]
    	
    pdf_final = pdf.copy()
	
	for col in typesdf:
    subtypes = {k:v for k,v in types.items() if k in [col]}
    try:
        pdf_final = pdf_final.astype(subtypes)
    except:
        pass