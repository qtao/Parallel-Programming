#!/usr/bin/env python

import fileinput

def main():
    current_key = None
    neighbor = []
    connections = []
    for line in fileinput.input():
        nodes = line.strip().split()
        key = nodes[0]
        if not current_key == None and not key == current_key:
            for conn in connections:
                if conn[0] in neighbor and conn[1] in neighbor:
                    print '<' + current_key + ',' + conn[0] + ',' + conn[1] + '>'
            del neighbor[:]
            del connections[:]
        current_key = key
        del nodes[0]
        if nodes[0] == key:
            neighbor.append(nodes[1])
        elif nodes[1] == key:
            neighbor.append(nodes[0])
        else:
            connections.append(nodes)

if __name__ == '__main__':
    main()