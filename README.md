KNIME provides powerful and flexible means to mine data. However, as many methods are implemented just for data modeling languages like R or MATLAB, it is crucial to integrate these languages into KNIME. To some extent this is already possible. However, from our daily work we have learned that many users need to use scripts without having any background in scripting. Thus we implemented a new open source scripting integration framework for KNIME, which is based on RGG templates <sup>[1][RGG: A general GUI Framework for R scripts; Ilhami Visne, Bioinformatics, 2009, 10:74]</sup>. Its main purpose is to hide the script complexity behind a user-friendly graphical interface. Furthermore, our approach goes beyond the existing integration of R as it provides better and more flexible graphics support, flow variable support and an easy-to-extend server-based script template repository.


[RGG: A general GUI Framework for R scripts; Ilhami Visne, Bioinformatics, 2009, 10:74]: https://bmcbioinformatics.biomedcentral.com/articles/10.1186/1471-2105-10-74


## Useful Links
* [The KNIME framework](www.knime.org)
* [KNIME community contributions]
* [RGG fork](https://github.com/knime-mpicbg/rgg)
* [Scripting Templates](https://github.com/knime-mpicbg/scripting-templates)


## Development
Since KNIME is an Eclipse application it is easiest to use that IDE. Follow the instruction on [KNIME SDK](https://github.com/knime/knime-sdk-setup) repository to install and confige Eclipse for KNIME development.


To work on this project use `File → Import → Git → Projects from Git File → Clone URI` and enter this repositorie's URL.


### Debug Configuration:

In the main menu of Eclipse go to `Run → Debug Configurations... → Eclipas Application → KNIME Analytics Platform` and hit `Debug`.

You might want to change the memory settings in the `Arguments` tab of the debug configuration by adding:

    -XX:MaxPermSize=256m


## Installation
Once KNIME is installed you have the following possibilities:

1. The easiest is to use the p2 update mechanism of KNIME (Help > Install new Software). Find the detailed instructions on the [KNIME Community Contributions](https://www.knime.com/community/scripting) website.
2. Use eclipse to build the plugins yourself and add them to the plugin directory of the KNIME installation.



# License
Copyright (c) 2010, Max Planck Institute of Molecular Cell Biology and Genetics, Dresden
All rights reserved.

Source code contained in the directories groovy4knime, python-server, pyhton4knime and r4knime is distributed under GPL3 license.

Source code contained in the directories matlab-server and the "matlab4knime" nodes are distributed under the BSD license.


 
<br/><br/>
<sup>1: [RGG: A general GUI Framework for R scripts; Ilhami Visne, Bioinformatics, 2009, 10:74]</sup>

[KNIME Community Contributions]: https://www.knime.com/community/scripting
