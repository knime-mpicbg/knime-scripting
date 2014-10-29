import platform
import csv
import array
from functools import partial
    
# Check for the correct version
version = platform.python_version()
if float(version[:3]) < 2.7:
    from ordereddict import OrderedDict
else:
    from collections import OrderedDict

# python 3 compat
if float(version[:3]) > 3.0:    
    NoneType = None
    StringType = str
    FloatType = float
    IntType = int
    LongType = int
else:
	from types import *	
	
# open() function for py2/3
def openf(filename, mode, **kwargs):
    return open(filename, mode, **kwargs) if float(version[:3]) < 3 else open(filename, mode[0], newline='', **kwargs)
    
# test if pandas is available
try:
    import pandas as pd
    import numpy as np
    have_pandas = True    
except:
    have_pandas = False


# For some large CSV files one may get the exception
# Error: field larger than field limit (131072)
# This is a quick and dirty solution to adapt the csv field_size_limit.
#
# Taken from:
# http://stackoverflow.com/questions/15063936/csv-error-field-larger-than-field-limit-131072
import sys
maxInt = sys.maxsize if sys.version_info >= (3, 0) else sys.maxint
decrement = True

while decrement:
    # decrease the maxInt value by factor 10 
    # as long as the OverflowError occurs.

    decrement = False
    try:
        csv.field_size_limit(maxInt)
    except OverflowError:
        maxInt = int(maxInt/10)
        decrement = True

#
#  Reads the first 'count' lines from a CSV file and determines the type of
#  each column.  Returns a dictionary that maps column names to their data type.
#
#  Example use:
#    try:
#       types = infer_column_types(csv_filename, 100)
#       table = create_data_table(...);
#    except:
#       types = infer_column_types(csv_filename, 0)
#       table = create_data_table(...);
#
def infer_column_types(csv_filename, count):
    csv_reader = csv.reader(openf(csv_filename, 'rb'), delimiter=',', quotechar='"')
    types = OrderedDict()
    current = 0
    for row in csv_reader:
        # The first row contains column names.  Use them to build the dictionary keys.
        if current == 0:
            for item in row:
                types[item] = NoneType

        elif current < count or count == 0:
            index = 0
            for item in types:
                t = types[item]
                try:
                    int(row[index])
                    if t == NoneType:
                        t = IntType
                except:
                    try:
                        float(row[index])
                        if t == NoneType or t == IntType:
                            t = FloatType
                    except:
                        t = StringType
                types[item] = t
                index += 1
        current += 1
    return types

#
#  Create a table with a properly sized empty list for each column
#
def create_empty_table(csv_filename, types, header_lines):
    table = OrderedDict()
    count = line_count(csv_filename) - header_lines

    for item in types:
        # init table with empty lists. The previous approach to init it with 
        # count * [None] entries led to problems with multi-line CSVs. For 
        # example, if there was only one entry in the CSV but this entry was
        # a multi-line string of n lines, the previous approach would have
        # created a size n list, but create_data_table would have only filled
        # the first entry, leaving entries 1..n as None. 
        table[item] = []

    return table

#
#  Build a dictionary that contains a mapping from column name to column type.
#  Assumes the first line of the file contains comma-separated names and the second line
#  contains comma-separated types, e.g. INT, FLOAT, STRING
#
def get_column_types(csv_filename):
    csv_reader = csv.reader(openf(csv_filename, 'rb'), delimiter=',', quotechar='"')
    types = OrderedDict()
    current = 0
    for row in csv_reader:
        # The first row contains column names.  Use them to build the dictionary keys.
        if current == 0:
            for item in row:
                types[item] = NoneType
        # The second row contains column types.  Use them to build the dictionary values.
        elif current == 1:
            index = 0
            for item in types:
                itemType = row[index]
                if itemType == "INT":
                    t = IntType
                elif itemType == "FLOAT":
                    t = FloatType
                else:
                    t = StringType

                types[item] = t
                index += 1

            return types

        current += 1

#
#  Create a data table with values from a CSV table.  The types parameter
#  is a dictionary mapping the column name to its type as created by get_column_types
#
def create_data_table(csv_filename, types, header_lines):
    
    # if pandas is available, use it!
    if have_pandas:
        skip = range(1, header_lines)
        d = pd.read_csv(csv_filename, skiprows=skip).to_dict()
        d = OrderedDict({k: list(d[k].values()) for k in d}) # convert to dict of lists (as used by the python snippet)
        return d
    else:
        table = create_empty_table(csv_filename, types, header_lines)
    
        csv_file = openf(csv_filename, 'rb')
    
        csv_reader = csv.reader(csv_file, delimiter=',', quotechar='"')
    
        current = -header_lines
        for row in csv_reader:
            if current >= 0:
                index = 0
                for item in types:
                    array = table[item]
                    t = types[item]
                    value = row[index]
                    if value == "":
                        value = None
                    elif t == FloatType:
                        value = float(value)
                    elif t == IntType:
                        value = int(value)
    
                    # Instead of setting array[current]=value we append
                    # to the array since the array was initialized as an
                    # empty list in create_empty_table
                    array.append(value)
    
                    index += 1
            current += 1

    return table

#
#  Return the number of data lines in the file
#
def line_count(csv_filename):
    # Count the number of lines
    csv_file = openf(csv_filename, 'rb')
    return sum(1 for line in csv_file)
    csv_file.close()


#
#  Read a CSV file into an OrderedDict.  The first line of the file is assumed to be the column names.
#  If read_types is True the second line must contain the column types, otherwise the file will be read
#  to infer the types.
#
def read_csv(csv_filename, read_types):
    if read_types:
        types = get_column_types(csv_filename)
        table = create_data_table(csv_filename, types, 2)
    else:
        try:
            types = infer_column_types(csv_filename, 100)
        except:
            types = infer_column_types(csv_filename, 0)

        table = create_data_table(csv_filename, types, 1)

    return table

#
#  Write a table (dictionary) to a csv file.  The first line will contain a comma-separated list
#  of column names and the second line will contain a comma-separated list of data types.  All subsequent
#  lines will be a row with comma-separated values for each colunn.
#
def write_csv(csv_filename, table, write_types):
    csv_file = openf(csv_filename, 'wb')
    csv_writer = csv.writer(csv_file, delimiter=',', quotechar='"', quoting=csv.QUOTE_NONNUMERIC)

    keys = list(table) 
    count = len(table[keys[0]])

    #  First write the column headers
    csv_writer.writerow(keys)

    #  Next save the types if desired
    if write_types:
        types = []
        for item in table:
            column = table[item]
            index = 0
            col = None

            # Find the first non-missing value
            for index, val in enumerate(column):
                if type(val) is not NoneType:
                    break
                
            if have_pandas: # pandas is using np types
                if np.issubdtype(type(val), np.int) or np.issubdtype(type(val), np.long):
                    types.append("INT")
                elif np.issubdtype(type(val), np.float):
                    types.append("FLOAT")
                else:
                    types.append("STRING")
            else:
                if type(val) is IntType or type(val) is LongType:
                    types.append("INT")
                elif type(val) is FloatType:
                    types.append("FLOAT")
                else:
                    types.append("STRING")

        csv_writer.writerow(types)

    #  Next write the data values, row by row
    for current in range(count):
        row = []
        for item in table:
            column = table[item]
            value = column[current]
            if value is not None:
                row.append(value)
            else:
                row.append("")

        csv_writer.writerow(row)

    csv_file.close()