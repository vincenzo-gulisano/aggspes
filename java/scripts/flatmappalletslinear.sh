folder=data/output_files/flatmappallets/
duration=120000
batch_size=1
allowed_lateness=30000
impl_types=(NATIVE NATIVE NATIVE NATIVE NATIVE NATIVE SINGLEOUT SINGLEOUT SINGLEOUT SINGLEOUT SINGLEOUT SINGLEOUT MULTIOUT MULTIOUT MULTIOUT MULTIOUT MULTIOUT MULTIOUT)
exp_ids=(llf alf hlf lhf ahf hhf llf alf hlf lhf ahf hhf llf alf hlf lhf ahf hhf) 
start_rates=(10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10 10)
end_rates=(400 200 200 210 190 190 300 80 30 110 80 35 350 180 180 200 165 160)
experiments_per_rate=10

mkdir -p ${folder}/

if [ ! -f "${folder}/log.txt" ]; then
    echo "folder,repetition,rate,sleepTime,outcome" >> ${folder}/log.txt
fi

for t in ${!impl_types[@]}; do

	type=${types[$t]}
	
	exp_id=${exp_ids[$t]}
	echo "Starting experiments for exp_id:" $exp_id
	
	for r in {0..2}; do

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

			# Check if the experiments has already been reported
			if [ ! -z $(grep "${exp_folder},${r},${rate},${sleepTime},1" "${folder}/log.txt") ]; then 
				echo "This experiment has been already reported as successful!";
			elif [ ! -z $(grep "${exp_folder},${r},${rate},${sleepTime},0" "${folder}/log.txt") ]; then 
				echo "This experiment has been already reported as not successful!";
			else

				echo "Creating/cleaning folders"
				mkdir -p ${exp_folder}

				duration_seconds=$(( $duration / 1000 ))

				# Create classpath variable
				mvn dependency:build-classpath -Dmdep.outputFile=tmp.txt
				CLASSPATH=$(cat tmp.txt)
				java -classpath ./target/aggregates_for_the_win-1.0-SNAPSHOT.jar:$CLASSPATH usecase.pallets.QueryFlatMap --inputFile data/input_files/pallets/ --outputFile ${exp_folder}/flatmap_native.csv --injectionRateOutputFile ${exp_folder}/flatmap_native_injectionRate.csv --throughputOutputFile ${exp_folder}/flatmap_native_throughput.csv --latencyOutputFile ${exp_folder}/flatmap_native_latency.csv --duration ${duration} --batchSize ${batch_size} --sleepTime ${sleepTime} --experimentID ${exp_id} --outputRateOutputFile ${exp_folder}/flatmap_native_outputRate.csv --maxLatencyViolations 3 --maxLatency 15000 --implementationType ${type}

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
