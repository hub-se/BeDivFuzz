5 10 15 or 30 parallel 



each script gets an id and the total number of parallel runs

e.g., 5 parallel runs:
id 1: run 1-6
id 2: run 7-12
id 3: run 13-18
id 4: run 19-24
id 5: run 25-30

lower = (id-1) * (n_total/n_parallel) + 1
upper = (id) * (n_total/n_parallel)


