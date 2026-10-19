#!/usr/bin/env python3
"""Check translation completeness and formatting contracts for every declared app locale."""
from collections import Counter
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

RES = Path(__file__).resolve().parents[1] / 'app/src/main/res'
ANDROID = '{http://schemas.android.com/apk/res/android}'
errors = []

def resources(folder):
    result = {}
    for path in sorted(folder.glob('*.xml')):
        for node in ET.parse(path).getroot():
            if node.tag not in ('string', 'string-array', 'plurals'):
                continue
            key = node.attrib['name']
            if key in result:
                errors.append(f'{folder.name}: duplicate resource {key}')
            result[key] = node
    return result

def text(node):
    return ''.join(node.itertext())

def placeholders(value):
    return Counter(re.findall(r'%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z]', value.replace('%%', '')))

def compare(key, source, target, locale):
    if placeholders(source) != placeholders(target):
        errors.append(f'{locale}: placeholder mismatch in {key}')

base = resources(RES / 'values')
tags = [n.attrib[ANDROID + 'name'] for n in ET.parse(RES / 'xml/locale_config.xml').getroot()]
for tag in tags[1:]:
    parts = tag.split('-')
    if len(parts) == 1:
        qualifier = tag
    elif len(parts) == 2 and (len(parts[1]) == 2 or parts[1].isdigit()):
        qualifier = parts[0] + '-r' + parts[1]
    else:
        qualifier = 'b+' + '+'.join(parts)
    folder = RES / ('values-' + qualifier)
    translated = resources(folder)
    for key, node in base.items():
        if node.get('translatable') == 'false':
            if key in translated:
                errors.append(f'{tag}: English-only resource overridden: {key}')
            continue
        other = translated.get(key)
        if other is None:
            errors.append(f'{tag}: missing {key}')
            continue
        if node.tag != other.tag:
            errors.append(f'{tag}: resource type mismatch {key}')
        elif node.tag == 'string':
            compare(key, text(node), text(other), tag)
        elif node.tag == 'string-array':
            if len(node) != len(other):
                errors.append(f'{tag}: array length mismatch {key}')
            for i, (a, b) in enumerate(zip(node, other)):
                compare(f'{key}[{i}]', text(a), text(b), tag)
        else:
            source_other = next(n for n in node if n.get('quantity') == 'other')
            quantities = {n.get('quantity') for n in other}
            required = ({'one', 'many', 'other'} if tag in ('pt-BR', 'fr', 'es') else ({'one', 'other'} if tag == 'de' else {'other'}))
            if not required <= quantities:
                errors.append(f'{tag}: missing plural categories {key}: {required - quantities}')
            for n in other:
                compare(f'{key}/{n.get("quantity")}', text(source_other), text(n), tag)
    for key in translated.keys() - base.keys():
        errors.append(f'{tag}: unknown key {key}')

for folder in ('layout', 'menu', 'xml'):
    for path in (RES / folder).glob('*.xml'):
        for node in ET.parse(path).iter():
            for attr in ('text', 'hint', 'contentDescription', 'title', 'summary'):
                value = node.get(ANDROID + attr)
                if value and not value.startswith(('@', '?')):
                    errors.append(f'{path.name}: hardcoded {attr}: {value}')

if errors:
    print('\n'.join(errors))
    sys.exit(1)
print(f'Localization contracts passed: {len(base)} resources, {len(tags)} languages.')
