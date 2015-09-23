#!/usr/bin/env python

import os, sys
import argparse
import getpass
import json, csv
import urllib, httplib
import HTMLParser
import requests

from requests.auth import HTTPBasicAuth

def get_json(header, row):
    id_index = header.index('georeferencer_id')
    thumbnail_index = header.index('thumbnail')
    title_index = header.index('title')
    num_gcps_index = header.index('num_gcps')
    status_index = header.index('status')
    out = {}
    out['id'] = row[id_index]
    out['thumbnail'] = row[thumbnail_index]
    out['image_url'] = row[thumbnail_index]
    out['metadata'] = {}
    out['metadata']['title'] = row[title_index]
    out['metadata']['num_gcps'] = row[num_gcps_index]
    out['metadata']['georeferenced'] = row[status_index].strip() == 'georeferenced'
    return out

parser = argparse.ArgumentParser(description='Tool which upload data from StareMapy.cz to imagesearch.mzk.cz')
parser.add_argument('--username', type=str, help='Username which is used to log in into imagesearch.mzk.cz')
parser.add_argument('--password', type=str, help='Password which is used to log in into imagesearch.mzk.cz')

args = parser.parse_args()

username = args.username
password = args.password

if not args.username:
    username = raw_input("username: ")
if not args.password:
    password = getpass.getpass("password: ")

query = "SELECT * FROM 1gpUBes9cpOkH3h-fNyS_4dnuLelIY-bSuWdNfqw"
params = urllib.urlencode({'query' : query})
conn = httplib.HTTPSConnection("www.google.com")
conn.request("GET", "/fusiontables/exporttable?%s" % params)
response = conn.getresponse()
if response.status == 200:
    htmlParser = HTMLParser.HTMLParser()
    csvreader = csv.reader([htmlParser.unescape(row.decode("utf-8")).encode("utf-8") for row in response.read().split('\n')])
    header = None
    i = 1
    j = 0
    request = []
    for row in csvreader:
        if header is None:
            header = list(row)
        else:
            if row:
                json_data = get_json(header, row)
                if json_data['image_url'].split():
                    request.append(json_data)
                    print("Map {} was processed".format(i))
                    i += 1
                    j += 1
        if j == 50:
            j = 0
            r = requests.post('http://imagesearch.mzk.cz/v1/ingest', auth=HTTPBasicAuth(username, password), data=json.dumps(request))
            print(r.json())
            request = []
    if j > 0:
        r = requests.post('http://imagesearch.mzk.cz/v1/ingest', auth=HTTPBasicAuth(username, password), data=json.dumps(request))
        print(r.json())

    r = requests.post('http://imagesearch.mzk.cz/v1/commit', auth=HTTPBasicAuth(username, password))
    print(r.json())

else:
    print("Response code: {}".format(response.status))
