# aggspes

This is the repository for the code used in the paper "On the Semantic Overlap of Operators in Stream Processing Engines".
You can find a PDF of it [here](https://drive.google.com/file/d/1gAmvWTLMAFkwJapby6lXmc3equSXu0yt/view?usp=share_link).

**SETUP**

For the input data, please check the paper. For convienence, the actual files used for the experiments can be downloaded from here

To run experiments, you only need to download and run mvn clean package in the java folder

You can then run the experiments by executing one of the following scripts:

./scripts/flatmapwikipediasearch.sh  
./scripts/flatmapwikipedialinear.sh  
./scripts/flatmappalletssearch.sh  
./scripts/flatmappalletslinear.sh  
./scripts/joinwikipediasearch.sh  
./scripts/joinwikipedialinear.sh  
./scripts/joinpalletssearch.sh  
./scripts/joinpalletslinear.sh  

The scripts are named so that:
- if the name contains join, then the experiment is about the J operator; if the name contains flatmap, then the experiment is about the FM operator.
- if the name contains wikipedia, then the experiment is from the Wikipedia use case (used in the paper for the high-end server); if the name contains pallets, then the experiment is from the 2D Scans use case (used in the paper for the Odroid device).
- if the name contains linear, then each experiment starts with the given injection rate and grows by the given step up to the desired max injection rate; if the name contains search, then each experiment starts with the given injection rate (which should be one that cannot be sustained) and reduces/increases by half (of the previous rate) the rate of the new experiment, until the change is below a given threshold.

*NOTICE:* The thresholds in this repository are based on the hardware used to run the experiments described in the paper. If different hardware is used, the values should be adjusted accordingly

**PRODUCING GRAPHS**

Finally, to reproduce the graphs presented in the paper, please use the Jupyter Notebook from the jupyter folder. The notebook contains comments to help producing the graphs.
