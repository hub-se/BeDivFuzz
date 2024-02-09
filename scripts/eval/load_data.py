# Implementation adapted from: https://github.com/sameerreddy13/rlcheck/blob/master/scripts/load_data.py

import os
import numpy as np

class DataLoader:

	generators = ['zest', 'bedivfuzz', 'tracking']
	validity = ['ant', 'rhino', 'closure', 'maven', 'bcel']

	sm = {
		0:	'unix_time',
		1:	'cycles_done',
		2:	'cur_path',
		3:	'paths_total',
		4:	'pending_total',
		5:	'pending_favs',
		6:	'map_size',
		7:	'unique_crashes',
        9:	'unique_hangs',
        10:	'max_depth',
        11:	'execs_per_sec',
		12:	'valid_inputs',
		13:	'invalid_inputs',
		14:	'valid_cov',
		15:	'all_covered_probes',
		16:	'valid_covered_probes',
		17:	'num_coverage_probes',
		18:	'covered_semantic_probes',
		19:	'num_semantic_probes',
		20:	'b0',
		21:	'b1',
		22:	'b2'
	}

	reverse_entries = [(kv[1], kv[0]) for kv in sm.items()]
	sm.update(reverse_entries) 

	def __init__(self, E):
		'''
        E: base directory for experiments
		Data dict becomes dictionary such that

		data_dict[{experiment name}] = numpy array with entry i equal to data for one run 
									length = num runs and 
									entry shape of num_lines x 7

		data_dict has dictionary entry for each experiment
		'''
		self.E = E 
		self.data_dict = {}
		self.compute_plot_data_step()

	def compute_plot_data_step(self):
		'''
		Determines a suitable plot data slice step based on the min. number of data points in a log.
		Each plot data file will be sliced with the same value to ensure equal numbers of data points.  
		'''

		# First, we determine the minimum number of data points (rows) in a plot_data file
		row_numbers = []
		for d in os.listdir(self.E):
			if '.' in d: continue
			p = self.join(self.E, d, 'plot_data')
			with open(p, 'r') as fp:
				n = fp.read().count("\n")
				row_numbers.append(n)

		min_rows = min(row_numbers)

		steps = [100, 50, 25, 10, 5, 2]

		# Then, we identify the largest possible slice step s.t. we get at least 10 data points per file
		try:
			pd_step = next(x for x in steps if min_rows//x >= 10)
		except StopIteration as err:
			print('No suitable step value for data found.')
			print(err)
		self.plot_data_step = pd_step

	def load_data(self):
		for d in os.listdir(self.E):
			if '.' in d: continue
			data = []
			target = self.join(self.E, d)
			run_data = self.parse_plot_data(target)
			data.append(np.array(run_data, dtype=float))

			self.data_dict[d] = np.array(data)

	def get_data(self, base_dirname, replay):
		'''
		gen = one of quickcheck, zest, rl, bediv-simple, bediv-structure
		validity = one of ant, maven, closure, rhino, nashorn, tomcat
		'''
		dirnames = [base_dirname + "-{}{}".format(i, "-replay" if replay else "") for i in range(1,16) ]
		data = [v[0] for d,v in self.data_dict.items() if d in dirnames]
		return data

	def join(self, *args):
		return "/".join(args)

	def parse_plot_data(self, target):
		p = self.join(target, 'plot_data')

		with open(p, 'r') as fp:
			lines = fp.readlines()
			del lines[0]

			# We normalize all plot_data recordings to equal length, see self.compute_plot_data_step
			lines = lines[0:len(lines):len(lines)//self.plot_data_step][:self.plot_data_step]
                        
		results = []
		for l in lines:
			l = str(l).replace('\n', "")
			l = str(l).replace('Infinity', "0")
			l = str(l).replace('NaN', "0")

			stats = [float(x) for x in l.split(',')]
			for i, s in enumerate(stats):
				try:
					results[i].append(s)
				except IndexError:
					results.append([s])
		return np.array(results)
