#!/usr/bin/env python
import urllib.request
import json

import argparse
import signal
import sys
from decimal import Decimal, ROUND_HALF_UP
from time import monotonic
    
def sigintHandler(sig, frame):
    print('Ctrl+C was pressed, exiting...')
    sys.exit(0)

signal.signal(signal.SIGINT, sigintHandler)

promptModelLoad = ['{"model": "', '", "temperature": 0.0, "response_format": {"type": "json_object"}, "messages": [{"role": "system", "content": "Respond in json to the user."}, {"role": "user", "content": "Hi!"}]}']

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

def log(str):
  print(str)

# call to LLM /v1/chat/completions API
# it expects server and the JSON message as string
# returns dict with message content and duration of the call, or error 
def v1ChatCompletions(server, msg):
  duration = None
  try:
    startTime = monotonic()
    res = urllib.request.urlopen(urllib.request.Request('http://' + server + '/v1/chat/completions', data=msg.encode('utf-8'), method='POST', headers={'Content-Type':'application/json'}), timeout=None).read()
    duration = float(Decimal(monotonic() - startTime).quantize(Decimal('0.001'), ROUND_HALF_UP))
    resStr = json.loads(res)['choices'][0]['message']['content']
    return {'msg': resStr, 'duration': duration}
  except Exception as e:
    err = '{}: {}'.format(type(e).__name__, str(e))
    log(err)
    return {'err': err, 'duration': duration}

def main():
  argParser = argparse.ArgumentParser(description='Runs test by issuing requests to /v1/chat/completions API for different models with prompts specified via configuration. Creates log file and one data or error file for each request.')
  argParser.add_argument('-s', '--server', help="Server's hostname or IP address and port in form: server:port (default=10.109.212.20:11434)", type=str, default='10.109.212.20:11434')
  argParser.add_argument('-p', '--prefix', help='Prefix to use for filenames that test will generate (default=llms-test)', type=str, default='llms-test')
  argParser.add_argument('-c', '--config', help='Config file containing JSON structure with prompts, models and terms', type=str)
  args = argParser.parse_args()
  server = args.server
  prefix = args.prefix
  config = args.config
  # load configuration from a file if provided
  if config != None:
     log('Loading configuration from: {}'.format(config))
     fConfig = open(config, 'r', encoding='utf-8')
     configDict = json.load(fConfig)
     fConfig.close()
     global prompts
     global models
     global terms
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
  # open file to log test progress
  fLog = open('{}.log.jsonl'.format(prefix), 'w', encoding='utf-8')
  for model in models:
    # first issue a "greeting" prompt to ensure that model is loaded into GPUs
    log('--- Model: {}'.format(model))
    ret = v1ChatCompletions(server, promptModelLoad[0] + model + promptModelLoad[1])
    log('{}\nModel load & greet time: {}s'.format(ret.get('msg'), ret.get('duration')))
    for term in terms:
      termName = next(iter(term))
      log('Term: "{}"\nContext: "{}"'.format(term[termName]['t'], '" > "'.join(term[termName]['c'])))
      for prompt in prompts:
        promptName = next(iter(prompt))
        log('Prompt: {}'.format(promptName))
        ret = v1ChatCompletions(server, prompt[promptName][0] + model + prompt[promptName][1] + term[termName]['t'] + prompt[promptName][2] + '\\" > \\"'.join(term[termName]['c']) + prompt[promptName][3])
        err = ret.get('err')
        duration = ret.get('duration')
        if err == None:
          f = open('{}.{}.{}-{}.data'.format(prefix, model.replace(':', '-'), termName, promptName), 'w', encoding='utf-8')
          f.write(ret.get('msg'))
          f.close()
        else:
          log('Error: {}'.format(err, ret.get('duration')))
          f = open('{}.{}.{}-{}.err'.format(prefix, model.replace(':', '-'), termName, promptName), 'w', encoding='utf-8')
          f.write(err)
          f.close()
        if duration != None:
          log('Duration: {}s'.format(ret.get('duration')))
          json.dump({'model': model, 'term': termName, 'prompt': promptName, 'duration': duration, 'error': (err != None)}, fLog)
          fLog.write('\n')
          fLog.flush()
        else:
          json.dump({'model': model, 'term': termName, 'prompt': promptName, 'error': (err != None)}, fLog)
          fLog.write('\n')
          fLog.flush()
  fLog.close()

if __name__ == '__main__':
    main()