import pandas as pd
import platform
import csv
import shelve

# Check for the correct version
version = platform.python_version()

# open() function for py2/3
def openf(filename, mode, **kwargs):
    return open(filename, mode, **kwargs) if float(version[:3]) < 3 else open(filename, mode[0], newline='', **kwargs)

# Read a CSV file into a pandas dataframe.  
# The first line of the file is assumed to be the column names.
# The second line must contain the column types, column types will be inferred first then
# the attempt to convert them follows (column type stays unchanged if conversion failed)
#
def read_csv(csv_filename):
	# read data with column headers, row ids and infer data types
	pdf = pd.read_csv(csv_filename, skiprows = [1], sep = ',', header = 0, index_col = 0)
	# extract expected data types
	typesdf = pd.read_csv(csv_filename, sep=',', nrows = 1)
	typesdf = typesdf.drop('Row ID', axis = 1)
	types = dict()	
	for k in typesdf:
		types[k] = typesdf.iloc[0][k]
		
	# try to apply column types, pass if it fails
	for col in typesdf:
		subtypes = {k:v for k,v in types.items() if k in [col]}
		try:
			if subtypes[col] == 'timedelta64[ns]':
				pdf[col] = pd.to_timedelta(pdf[col], unit='ns')
			else:
				pdf = pdf.astype(subtypes)
		except:
			print("Read KNIME data as pandas data frame: failed to convert {}. Keep as {}".format(subtypes, pdf[list(subtypes.keys())[0]].dtypes))
			pass
	return pdf

# Write CSV file from pandas dataframe        
def write_csv(csv_filename, pdf):

	# need to filter dataframe for supported types
	include=['object','bool','float','int','datetime64[ns]', 'timedelta64[ns]']
	exclude = pdf.select_dtypes(exclude=include).columns.tolist()
	pyOut = pdf.select_dtypes(include)
	exportTypes = pyOut.dtypes.apply(lambda x: x.name).tolist()
	
		# make duration columns to isoformat string
	durationColumns = list(pyOut.select_dtypes(include=['timedelta64[ns]']))

	for col in durationColumns:
		pyOut[col] = pyOut[col].apply(lambda x: x.isoformat())
	
	if len(exclude) > 0:
		print("Column(s) with unsupported data type(s) will not be returned to KNIME: {}".format(', '.join(exclude)))
	
	header = pyOut.columns 
	header = header.insert(0, "Row ID") 
	
	types = []
    types.append("INDEX")
    types = types + exportTypes
		

	csv_file = openf(csv_filename, 'wb')
	csv_writer = csv.writer(csv_file, delimiter=',', quotechar='"')

	# First write the column headers and data types
	csv_writer.writerow(header)
	csv_writer.writerow(types)

	csv_file.close()
	
	# append data
	with openf(csv_filename, 'ab') as f:
		pyOut.to_csv(f, header=False, date_format='%Y-%m-%dT%H:%M:%S.%f', line_terminator = '\r\n')