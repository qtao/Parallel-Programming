#!/usr/bin/env python

import fileinput

def main():
    for line in fileinput.input():
        friends = line.strip().split()
        main_person = friends[0]
        for key in friends:
        	for node in friends:
	        	if not main_person == node and main_person > node:
	        		print key + ' ' + main_person + ' ' + node

if __name__ == '__main__':
    main()