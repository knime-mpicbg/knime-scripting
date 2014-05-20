% STARTSERVER
%
% The STARTSERVER unction launches a server instance. It has to be executed
% from within MATLAB.
% Without input it tries to find the jar-files in the current working
% directory, if nothing is found, the user is prompted by a dialog window
% to indicate the directory containing the MatlabWebServer binaries.
% Alternativeley The path can also be given as a input string.
%

% 18 November 2010
% Holger Brandl, Felix Meyenhofer
% Institution: Max Planck Institute for Molecular Cell Biology and Genetics


function mServer = startserver(varargin)


% Handle input.
parser = inputParser();
parser.addOptional('rootDirectory', cd, @(x)exist(x, 'dir'));
parser.parse(varargin{:});

% Get the jar file paths.
allLibs = getjarfiles(parser.Results.rootDirectory, 1);

% Prompt the user for input if nothing was found.
if isempty(allLibs)
    fprintf('\nChoose the directory of the jar-files from the matlab-server.\n')
    allLibs = getjarfiles(uigetdir);
end

% Make the binaries available.
for i = 1:numel(allLibs)
    javaaddpath(allLibs{i});
end

% Instanciate and start the server.
mServer = de.mpicbg.math.toolintegration.matlab.MatlabWebServer();



%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%



function paths = getjarfiles(dirName, maxDepth)
    if nargin < 2
        maxDepth = 1;
    end

    if strcmp(dirName(end), filesep)
        dirName = dirName(1:end-1);
    end

    if any(ismember(computer, {'PCWIN' 'PCWIN64'}))
        seperator = '\\';
    else
        seperator = '/';
    end

    paths{1} = dirName;
    isDir = true;
    currentPosition = 1;
    while true
        for m = currentPosition:numel(paths);
            if isDir(m)
                paths{m} = [paths{m} filesep];
                tmp = filterinvalid(dir(paths{m}));
                if numel(tmp) > 0
                    paths = [paths strcat(paths{m}, {tmp.name} ) ];
                    isDir = [ isDir; cat(1, tmp.isdir) ];
                end
            end
        end
        if ~any(isDir(currentPosition:end)) || (currentPosition <= maxDepth)
            break
        end
        currentPosition = currentPosition + m;
    end

    paths = paths(~isDir);
    fileNames = cellfun(@(x)regexp(x, ['[^' seperator ']+$'], 'match'),paths, 'UniformOutput', false);
    I = cellfun(@(x)isempty(regexp(x, '.jar', 'match', 'once')), cat(1,fileNames{:}));
    paths = paths(~I);



function out = filterinvalid(in)
    out = struct([]);
    m = 1;
    for n = 1:numel(in)
        if ~isempty(in(n).name);
            if ismember(in(n).name, {'.' '..' '.DS_Store'})
                continue
            end
        end
        out(m).name = in(n).name;
        out(m).isdir = in(n).isdir;
        m = m+1;
    end