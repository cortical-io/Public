#!/usr/bin/env python
import urllib.request
import json
import re

import argparse
import base64
import signal
import sys

from html import escape
    
def sigintHandler(sig, frame):
    print('Ctrl+C was pressed, exiting...')
    sys.exit(0)

signal.signal(signal.SIGINT, sigintHandler)

logs = {}

prompts = [
  {'a':['{"model": "', '", "temperature": 0.0, "response_format": {"type": "json_object"}, "messages": [{"role": "system", "content": "Fill in json structure {\\"d\\":[[],[],[],[],[],[],[],[]],\\"hc\\":[],\\"ct\\":[]} that contains term definition (d) in its context as array of exactly 8 paragraphs. Each paragraph is array of exactly 5 elements. Each element contains exactly one sentence. Add array of exactly 10 typical higher level contexts (hc) of term. Add array of exactly 30 highly specific context terms (ct) of term."}, {"role": "user", "content": "Term: \\"', '\\"\\nContext: \\"', '\\""}]}']},
  {'b':['{"model": "', '", "temperature": 0.0, "response_format": {"type": "json_object"}, "messages": [{"role": "system", "content": "Fill in json structure {\\"d\\":[[],[],[],[],[],[],[],[]],\\"hc\\":[],\\"ct\\":[]} that contains term definition (d) in its context as array of exactly 8 paragraphs. Each paragraph is array of 5 elements each containing exactly one sentence. Add array of exactly 10 typical higher level contexts (hc) of term. Add array of exactly 30 highly specific context terms (ct) of term."}, {"role": "user", "content": "Term: \\"', '\\"\\nContext: \\"', '\\""}]}']}
]

models = [
  'aya:8b',
  'aya:35b',
  'command-r:35b',
  'deepseek-v2:16b',
  'glm4:9b',
  'hermes3:8b',
  'hermes3:70b',
  'llama3.1:8b',
  'llama3.1:70b',
  'llama3.2:3b',
  'llama3.2-vision:11b',
  'llama3.2-vision:90b',
  'mistral-large:123b',
  'mistral-nemo:12b',
  'mistral-small:22b',
  'mixtral:8x7b',
  'mixtral:8x22b',
  'nemotron:70b',
  'phi3:14b',
  'qwen2.5:7b',
  'qwen2.5:14b',
  'qwen2.5:32b',
  'qwen2.5:72b',
  'solar:10.7b',
  'solar-pro:22b',
  'stablelm2:12b'
]

terms = [
  {'0': {'t':'conflict','c':['world', 'countries', 'political regimes']}},
  {'1': {'t':'conflict','c':['business', 'companies', 'competition']}},
  {'2': {'t':'organ','c':['human', 'body']}},
  {'3': {'t':'organ','c':['world', 'countries', 'cities', 'governance']}}
]

reportStart = '''<!DOCTYPE html><html lang="en"><head><title>Comparison of LLMs returning JSON structure</title><meta charset="UTF-8"><link href="https://fonts.googleapis.com/css?family=Open+Sans" rel="stylesheet"><style>
html { font-size: 1rem; font-family: "Open Sans", Arial, sans-serif; }
body { background: #fff; }
h1 { font-size: 1.1rem; }
pre { white-space: pre-wrap; word-break: keep-all; }
/* prompt model result */
.pmr {
  /* padding: 0 0.5rem 0 0.5rem; */
  margin: 0 0.2rem 0 0.2rem;
}
/* label */
.l {
  font-size: 1rem;
  font-weight: normal;
}
.s0 {
  background: #ff5555;
  color: #fff;
  text-decoration: line-through;
}
.s1 {
  background: #ffcccc;
}
.s2 {
  background: #ffcc55;
}
.s3 {
  background: #ffffcc;
}
.s4 {
  background: #ccffcc;
}
.s5 {
  background: #ccff55;
}
.s6 {
  background: #55ff55;
}
/* details tabs */
.dts {
	position: relative;
	justify-content: left;
	display: flex;
	flex-wrap: wrap;
}
/* details tab */
.dt {
	padding: 0.25rem 0.5rem;
	font-size: 1rem;
	color: #000;
	font-weight: 600;
	display: block;
	order: 0; /* tabs come first */
	background: #ddd;
	border-radius: 0.5rem 0.5rem 0 0;
	margin-right: 0.25rem;
	margin-top: 0.2rem;
	margin-bottom: 0;
	cursor: pointer;
	border: thin solid #000;
	&:hover, &:focus {
		background-color: #ccc;
	}
	&::-webkit-details-marker {
		display: none; /* hide the default arrow icon */
	}
}
/* details content */
.dc {
	order: 1; /* content comes after tabs */
	padding: 0.25rem;
  border: thin solid #000;
	width: 100%; /* make the content sit on it's own row */
	details {
		summary {
			font-weight: 600;
		}    
		margin: 1.5rem;
	}
}
/* details item */
.di {
	display: contents;
	&[open] {
		& > .dt {
			background: #aaa;
			color: #333;
		}
	}
}
details[open]::details-content { display: contents; }
</style></head><body>'''

heading = '<h1>Tests were run on Kubernetes with 2x NVIDIA Quadro RTX 8000 (48GB RAM) GPU cards using ollama:0.4.2 container in November 2024.</h1>'

description = ''

scoreDetails = '''<details class="di" name="tg1"><summary class="dt">Score info</summary><div class="dc"><p>Scoring requirements:</p><ul>
<li>Content returned by LLM is JSON object and contains keys d (definition), hc (higher contexts), ct (context terms).</li>
<li>Key d value type is array with 8 elements. Each element is array of 5 strings.</li>
<li>Key hc value type is array of 10 strings.</li>
<li>Key ct value type is array of 30 strings.</li>
</ul><p>Score values:</p><ul>
<li><span class="pmr s6">(6)</span> - JSON object conforms to requirements</li>
<li><span class="pmr s5">(5)</span> - two of d, hc, ct conform to requirements</li>
<li><span class="pmr s4">(4)</span> - one of d, hc, ct conforms to requirements</li>
<li><span class="pmr s3">(3)</span> - objects d, hc, ct are arrays</li>
<li><span class="pmr s2">(2)</span> - JSON structure contains objects d, hc, ct</li>
<li><span class="pmr s1">(1)</span> - retuned content is valid JSON</li>
<li><span class="pmr s0">(0)</span> - returned content is not valid JSON</li>
</ul></div></details>'''

def log(str):
  print(str)

def scoreContent(contentDict):
  if isinstance(contentDict, dict):
    d = contentDict.get('d')
    hc = contentDict.get('hc')
    ct = contentDict.get('ct')
    if d != None and hc != None and ct != None:
      if isinstance(d, list) and isinstance(hc, list) and isinstance(ct, list):
        score  = 3
        if len(d) == 8:
          valid = True
          for p in d:
            if not isinstance(p, list):
              valid = False
              break
            if len(p) != 5:
              valid = False
              break
            for s in p:
              if not isinstance(s, str):
                valid = False
                break
              if s == '':
                valid = False
                break
            if not valid:
              break
          if valid:
            score += 1
        if len(hc) == 10:
          valid = True
          for s in hc:
            if not isinstance(s, str):
              valid = False
              break
            if s == '':
              valid = False
              break
          if valid:
            score += 1
        if len(ct) == 30:
          valid = True
          for s in ct:
            if not isinstance(s, str):
              valid = False
              break
            if s == '':
              valid = False
              break
          if valid:
            score += 1
        return score
      return 2
  return 1

def getFormattedContent(promptLogDict):
  contentDict = promptLogDict.get('dict')
  if contentDict != None:
    stats = 'Content info: '
    prefix = 'JSON object contains '
    if isinstance(contentDict, dict):
      d = contentDict.get('d')
      hc = contentDict.get('hc')
      ct = contentDict.get('ct')
      if d != None:
        stats += prefix + 'key d='
        prefix = ', '
        if isinstance(d, list):
          stats += 'array({})['.format(len(d))
          for p in d:
            if isinstance(p, list):
              stats += 'array({}), '.format(len(p))
            else:
              stats += 'not-array, '
          stats = stats[:-2] + ']'
        else:
          stats += 'not-array'
      if hc != None:
        stats += prefix + 'key hc='
        prefix = ', '
        if isinstance(hc, list):
          stats += 'array({})'.format(len(hc))
        else:
          stats += 'not-array'
      if ct != None:
        stats += prefix + 'key ct='
        if isinstance(ct, list):
          stats += 'array({})'.format(len(ct))
        else:
          stats += 'not-array'
    else:
      stats += 'Not a JSON object.'
    return ' {}<pre>{}</pre>'.format(stats, escape(json.dumps(contentDict, indent=2)))
  else:
    return '<p class="err">*** ERROR: Not valid JSON structure. ***</p><pre>{}</pre>'.format(escape(promptLogDict.get('data')))

def main():
  global logs
  argParser = argparse.ArgumentParser(description='Generates HTML report from log and data files generated by llms-test.py script.')
  argParser.add_argument('-p', '--prefix', help='Prefix to use for reading input files and for HTML report file name (default=llms-test)', type=str, default='llms-test')
  argParser.add_argument('-c', '--config', help='Config file containing JSON structure with prompts, models and terms', type=str)
  args = argParser.parse_args()
  prefix = args.prefix
  config = args.config
  # load configuration from a file if provided
  if config != None:
     log('loading configuration from: {}'.format(config))
     fConfig = open(config, 'r', encoding='utf-8')
     configDict = json.load(fConfig)
     fConfig.close()
     global prompts
     global models
     global terms
     global heading
     global description
     configHeading = configDict.get('heading')
     if configHeading != None:
        log('Setting heading.')
        heading = configHeading
     configDescription = configDict.get('description')
     if configDescription != None:
        log('Setting description.')
        description = configDescription
     configPrompts = configDict.get('prompts')
     if configPrompts != None:
        log('Setting prompts: {}'.format(len(configPrompts)))
        prompts = configPrompts
     configModels = configDict.get('models')
     if configModels != None:
        log('Setting models: {}'.format(len(configModels)))
        models = configModels
     configTerms = configDict.get('terms')
     if configTerms != None:
        log('Setting terms: {}'.format(len(configTerms)))
        terms = configTerms
  # open log file and read logs
  log('Reading log file: {}.log.jsonl'.format(prefix))
  fLog = open('{}.log.jsonl'.format(prefix), 'r', encoding='utf-8')
  line = fLog.readline()
  while line:
    logDict = json.loads(line)
    fData = None
    if logDict.get('error'):
      fData = open('{}.{}.{}-{}.err'.format(prefix, logDict.get('model').replace(':', '-'), logDict.get('term'), logDict.get('prompt')), 'r', encoding='utf-8')
      error = fData.read()
      log('\tThere was error returned. Model: {} Term: {} Prompt: {} {}'.format(logDict.get('model'), logDict.get('term'), logDict.get('prompt'), error))
      tmpDict = {'duration': logDict.get('duration'), 'error': logDict.get('error'), 'data': error, 'score': 0}
    else:
      fData = open('{}.{}.{}-{}.data'.format(prefix, logDict.get('model').replace(':', '-'), logDict.get('term'), logDict.get('prompt')), 'r', encoding='utf-8')
      tmpDict = {'duration': logDict.get('duration'), 'error': logDict.get('error'), 'data': fData.read()}
      try:
        tmpDict['dict'] = json.loads(tmpDict.get('data'))
        tmpDict['score'] = scoreContent(tmpDict['dict'])
      except Exception as e:
        log('\tNot valid JSON content. Model: {} Term: {} Prompt: {} {}: {}'.format(logDict.get('model'), logDict.get('term'), logDict.get('prompt'), type(e).__name__, str(e)))
        tmpDict['score'] = 0
    fData.close()
    logTerm = logs.get(logDict.get('term'))
    if logTerm != None:
      logModel = logTerm.get(logDict.get('model'))
      if logModel != None:
        logModel[logDict.get('prompt')] = tmpDict
      else:
        logTerm[logDict.get('model')] = {logDict.get('prompt'): tmpDict}
    else:
      logs[logDict.get('term')] = {logDict.get('model'): {logDict.get('prompt'): tmpDict}}
    line = fLog.readline()
  fLog.close()
  # open HTML report file and write begginning of the report
  fReport = open('{}-report.html'.format(prefix), 'w', encoding='utf-8')
  fReportNoOriginalContent = open('{}-report-no-original-content.html'.format(prefix), 'w', encoding='utf-8')
  fReport.write(reportStart)
  fReportNoOriginalContent.write(reportStart)
  fReport.write(heading)
  fReportNoOriginalContent.write(heading)
  if description != '':
    fReport.write(description)
    fReportNoOriginalContent.write(description)
  fReport.write('<div class="dts">')
  fReportNoOriginalContent.write('<div class="dts">')
  termsResults = ''
  termsResultsNoOriginalContent = ''
  tgm = 0
  tgp = 0
  firstTerm = True
  for term in terms:
    termName = next(iter(term))
    if logs.get(termName) == None:
      continue
    tgm += 1
    modelResults = ''
    modelResultsNoOriginalContent = ''
    firstModel = True
    for model in models:
      if logs[termName].get(model) == None:
        continue
      tgp += 1
      tmpStr = '<details class="di" name="tgm{}"{}><summary class="dt">{} '.format(tgm, ' open' if firstModel else '', model)
      modelResults += tmpStr
      modelResultsNoOriginalContent += tmpStr
      firstModel = False
      promptResults = ''
      promptResultsNoOriginalContent = ''
      firstPrompt = True
      for prompt in prompts:
        promptName = next(iter(prompt))
        if logs[termName][model].get(promptName) == None:
          continue
        tmpStr = '<span class="pmr s{}">&nbsp;{}({})&nbsp;</span>'.format(logs[termName][model][promptName]['score'], promptName, logs[termName][model][promptName]['score'])
        modelResults += tmpStr
        modelResultsNoOriginalContent += tmpStr
        fileName = 'term({})-context({})-model({})-prompt({}).txt'.format(term[termName]['t'], '#'.join(term[termName]['c']), model.replace(':', '-'), promptName)
        resultDetails = '<a href="data:text/plain;charset=UTF-8;base64,{}" download="{}">Original content</a>{}'.format(base64.b64encode(repr(logs[termName][model][promptName]['data'])[1:-1].encode('utf-8')).decode('utf-8'), fileName.translate(fileName.maketrans(r'\/:*?"<>| ', '__________')), getFormattedContent(logs[termName][model][promptName]))
        promptResults += '<details class="di" name="tgp{}"{}><summary class="dt">{}</summary><div class="dc">{}</div></details>'.format(tgp, ' open' if firstPrompt else '', '<span class="l">Model:</span> {} <span class="l">Prompt:</span> {} <span class="l">Duration:</span> {}s'.format(model, promptName, logs[termName][model][promptName]['duration']), resultDetails)
        promptResultsNoOriginalContent += '<details class="di" name="tgp{}"{}><summary class="dt">{}</summary><div class="dc">{}</div></details>'.format(tgp, ' open' if firstPrompt else '', '<span class="l">Model:</span> {} <span class="l">Prompt:</span> {} <span class="l">Duration:</span> {}s'.format(model, promptName, logs[termName][model][promptName]['duration']), getFormattedContent(logs[termName][model][promptName]))
        firstPrompt = False
      modelResults += '</summary><div class="dc"><div class="dts">{}</div></div></details>'.format(promptResults)
      modelResultsNoOriginalContent += '</summary><div class="dc"><div class="dts">{}</div></div></details>'.format(promptResultsNoOriginalContent)

    termsResults += '<details class="di" name="tg2"{}><summary class="dt"><span class="l">Term:</span> "{}"<br><span class="l">Context:</span> "{}"</summary><div class="dc"><div class="dts">{}</div></div></details>'.format( ' open' if firstTerm else '',term[termName]['t'], '\" &gt; \"'.join(term[termName]['c']), modelResults)
    termsResultsNoOriginalContent += '<details class="di" name="tg2"{}><summary class="dt"><span class="l">Term:</span> "{}"<br><span class="l">Context:</span> "{}"</summary><div class="dc"><div class="dts">{}</div></div></details>'.format( ' open' if firstTerm else '',term[termName]['t'], '\" &gt; \"'.join(term[termName]['c']), modelResultsNoOriginalContent)
    firstTerm = False
  fReport.write('<details class="di" name="tg1" open><summary class="dt">Results ({} different terms in specific context)</summary><div class="dc"><div class="dts">{}</div></div></details>'.format(len(terms), termsResults))
  fReportNoOriginalContent.write('<details class="di" name="tg1" open><summary class="dt">Results ({} different terms in specific context)</summary><div class="dc"><div class="dts">{}</div></div></details>'.format(len(terms), termsResultsNoOriginalContent))
  modelsList = ''
  for model in models:
    modelsList += '<li><a href="https://ollama.com/library/{}" target="_blank">{}</a></li>'.format(model, model)
  promptsList = ''
  for prompt in prompts:
    promptName = next(iter(prompt))
    promptsList += '<li>{}:<pre>{}</pre></li>'.format(promptName, escape(prompt[promptName][0] + 'xyz' + prompt[promptName][1] + 'pqr' + prompt[promptName][2] + '\\" > \\"'.join(['abc', 'def', 'ghi']) + prompt[promptName][3]))
  tmpStr = '<details class="di" name="tg1"><summary class="dt">Models info ({} models)</summary><div class="dc"><ul>{}</ul></div></details>'.format(len(models), modelsList) + '<details class="di" name="tg1"><summary class="dt">Prompts info ({} prompt variations)</summary><div class="dc"><ul>{}</ul></div></details>'.format(len(prompts), promptsList)
  fReport.write(tmpStr)
  fReportNoOriginalContent.write(tmpStr)
  fReport.write(scoreDetails)
  fReportNoOriginalContent.write(scoreDetails)
  fReport.write('</div></body></html>')
  fReportNoOriginalContent.write('</div></body></html>')
  fReport.close()
  fReportNoOriginalContent.close()

if __name__ == '__main__':
    main()