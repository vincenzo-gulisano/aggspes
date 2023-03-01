folder=data/output_files/flatmappallets/
duration=120000
batch_size=1
allowed_lateness=30000
impl_types=(NATIVE NATIVE NATIVE NATIVE NATIVE NATIVE SINGLEOUT SINGLEOUT SINGLEOUT SINGLEOUT SINGLEOUT SINGLEOUT MULTIOUT MULTIOUT MULTIOUT MULTIOUT MULTIOUT MULTIOUT)
exp_ids=(llf alf hlf lhf ahf hhf llf alf hlf lhf ahf hhf llf alf hlf lhf ahf hhf)  
rates=(400 300 300 300 300 300 400 200 200 200 200 200 400 300 300 300 300 300)
max_diff=10

mkdir -p ${folder}/

if [ ! -f "${folder}/log.txt" ]; then
    echo "folder,repetition,rate,sleepTime,outcome" >> ${folder}/log.txt
fi

for t in ${!impl_types[@]}; do

	type=${impl_types[$t]}

	# for e in ${!exp_ids[@]}; do

		exp_id=${exp_ids[$t]}
		echo "Starting experiments for exp_id:" $exp_id
		
		for r in {0..0}; do

			echo "Repetition " $r

			# The initial rate to try for a certain implementation
			rate=${rates[$t]} #3000
			maxrate=$rate
			offset=$rate

			goOn=1

			while [ $goOn -eq 1 ]
			do

				# compute new offset
				let "offset=$offset/2"

				# computing rate from sleep time
				let "sleepTime=1000000000/$rate"
				echo "Trying rate " $rate " / sleepTime " $sleepTime

				exp_folder=${folder}/${exp_id}/${type}/${sleepTime}/${r}/

				experimentAlreadyRun=0
				# Check if the experiments has already been reported
				if [ ! -z $(grep "${exp_folder},${r},${rate},${sleepTime},1" "${folder}/log.txt") ]; then 
					echo "This experiment has been already reported as successful!";
					return=0
					experimentAlreadyRun=1
					# sleep 5
				elif [ ! -z $(grep "${exp_folder},${r},${rate},${sleepTime},0" "${folder}/log.txt") ]; then 
					echo "This experiment has been already reported as not successful!";
					return=13
					experimentAlreadyRun=1
					# sleep 5
				else

					echo "Creating/cleaning folders"
					mkdir -p ${exp_folder}

					duration_seconds=$(( $duration / 1000 ))

					# Create classpath variable
					mvn dependency:build-classpath -Dmdep.outputFile=tmp.txt
					CLASSPATH=$(cat tmp.txt)
					java -classpath ./target/aggregates_for_the_win-1.0-SNAPSHOT.jar:$CLASSPATH usecase.pallets.QueryFlatMap --inputFile data/input_files/pallets/ --outputFile ${exp_folder}/flatmap_native.csv --injectionRateOutputFile ${exp_folder}/flatmap_native_injectionRate.csv --throughputOutputFile ${exp_folder}/flatmap_native_throughput.csv --latencyOutputFile ${exp_folder}/flatmap_native_latency.csv --duration ${duration} --batchSize ${batch_size} --sleepTime ${sleepTime} --experimentID ${exp_id} --outputRateOutputFile ${exp_folder}/flatmap_native_outputRate.csv --maxLatencyViolations 3 --maxLatency 15000 --implementationType ${type}

					return=$?

				fi

				if [ $return -eq 13 ]; then
					echo "The rate is not sustainable, decreasing it"
					let newRate=$rate-$offset
					let newSleepTime=1000000000/$newRate
					echo "New rate " $newRate " / sleepTime " $newSleepTime
					if [ $experimentAlreadyRun -eq 0 ]; then
						echo "${exp_folder},${r},${rate},${sleepTime},0" >> ${folder}/log.txt
					fi
				else 
					echo "The rate is sustainable, increasing it"
					let newRate=$rate+$offset
					let newSleepTime=1000000000/$newRate
					echo "New rate " $newRate " / sleepTime " $newSleepTime
					if [ $experimentAlreadyRun -eq 0 ]; then
						echo "${exp_folder},${r},${rate},${sleepTime},1" >> ${folder}/log.txt
					fi
				fi
				
				# Compute difference
				let "diff = $rate - $newRate"
				# compute absolute difference (removing initial +/-, not the best...)
				let "abs_diff = ${diff##*[+-]}"
				echo "Difference between old and new rate: " $abs_diff
				if [ $abs_diff -le $max_diff ]; then
					echo "Stopping"
					goOn=0
				else 
					if [ $newRate -ge $maxrate ]; then
						echo "The new rate exceeds the max rate, so we stop"
						goOn=0
					else 
						echo "Trying new rate"
						goOn=1 
						let rate=$newRate
					fi
				fi



			done

		done

done