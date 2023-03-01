folder=data/output_files/joinwikipedia/
duration=600000
batch_size=1
allowed_lateness=30000
impl_types=(NATIVE NATIVE NATIVE NATIVE NATIVE NATIVE SINGLEOUT SINGLEOUT SINGLEOUT SINGLEOUT SINGLEOUT SINGLEOUT MULTIOUT MULTIOUT MULTIOUT MULTIOUT MULTIOUT MULTIOUT)
exp_ids=(LLJ ALJ HLJ LHJ AHJ HHJ LLJ ALJ HLJ LHJ AHJ HHJ LLJ ALJ HLJ LHJ AHJ HHJ) 
start_rates=(20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20)
end_rates=(1000 1000 1000 310 310 310 800 360 160 164 85 41 1000 1000 1000 340 340 340)
experiments_per_rate=4

mkdir -p ${folder}/

if [ ! -f "${folder}/log.txt" ]; then
    echo "folder,repetition,rate,sleepTime,outcome" >> ${folder}/log.txt
fi

for t in ${!impl_type[@]}; do

	type=${types[$t]}
	
	exp_id=${exp_ids[$t]}
	echo "Starting experiments for exp_id:" $exp_id
	
	for r in {0..0}; do

		echo "Repetition " $r

		# The initial rate to try for a certain implementation
		rate=${start_rates[$t]}
		maxrate=${end_rates[$t]}
		let step=($maxrate-$rate)/$experiments_per_rate

		goOn=1

		while [ $goOn -eq 1 ]
		do

			# computing rate from sleep time
			let sleepTime=1000000000/$rate
			echo "Trying rate " $rate " / sleepTime " $sleepTime

			exp_folder=${folder}/${exp_id}/${type}/${sleepTime}/${r}/

			if [ ! -z $(grep "${exp_folder},${r},${rate},${sleepTime},1" "${folder}/log.txt") ]; then 
				echo "This experiment has been already reported as successful!";
			elif [ ! -z $(grep "${exp_folder},${r},${rate},${sleepTime},0" "${folder}/log.txt") ]; then 
				echo "This experiment has been already reported as not successful!";
			else

				echo "Creating/cleaning folders"
				mkdir -p ${exp_folder}
			
				# Create classpath variable
				mvn dependency:build-classpath -Dmdep.outputFile=tmp.txt
				CLASSPATH=$(cat tmp.txt)
				java -classpath ./target/aggregates_for_the_win-1.0-SNAPSHOT.jar:$CLASSPATH usecase.wikipedia.QueryJoin --inputFile data/input_files/insertions.tsv --outputFile ${exp_folder}/join_native.csv --type native --performance true --injectionRateOutputFile ${exp_folder}/join_${type}_injectionRate.csv --throughputOutputFile ${exp_folder}/join_${type}_throughput.csv --latencyOutputFile ${exp_folder}/join_${type}_latency.csv --duration ${duration} --batchSize ${batch_size} --sleepTime ${sleepTime} --experimentID ${exp_id} --outputRateOutputFile ${exp_folder}/join_${type}_outputRate.csv --maxLatencyViolations 3 --maxLatency 15000 --implementationType ${type}

				return=$?

				if [ $return -eq 13 ]; then
					echo "${exp_folder},${r},${rate},${sleepTime},0" >> ${folder}/log.txt
				else 
					echo "${exp_folder},${r},${rate},${sleepTime},1" >> ${folder}/log.txt
				fi
			fi

			#update rate
			let rate=$rate+$step

			if [ $rate -gt $maxrate ]; then
				echo "Stopping"
				goOn=0
			fi

		done
	done
done
