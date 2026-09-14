#!/usr/bin/env python3
"""Run production Swift control flow with stand-ins for iOS-only SDK boundaries.

These tests do not emulate Screen Time daemon delivery or signed entitlements.
The Xcode build separately checks the real SDK integration.
"""
from pathlib import Path
import re
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
files = [
    'Shared/Constants.swift', 'Shared/SharedEnums.swift',
    'Shared/SharedBlockGroup.swift', 'Shared/ScheduleEvaluator.swift', 'Shared/WindowLog.swift', 'Shared/UsageBlockState.swift',
    'ZenLock/Core/Persistence/SelectionCoder.swift', 'ZenLock/Core/Persistence/AppGroupStorage.swift',
    'ZenLock/Core/Models/BlockGroup.swift', 'ZenLock/Features/Groups/GroupDraft.swift',
    'ZenLock/Core/Services/ShieldManager.swift', 'ZenLock/Core/Services/ActivityScheduleManager.swift',
    'ZenLock/Core/Services/BlockingService.swift',
    'ZenLock/Features/Groups/ScheduleToastFactory.swift',
    'DeviceActivityMonitorExtension/DeviceActivityMonitorExtension.swift',
]
with tempfile.TemporaryDirectory(prefix='zenlock-usage-tests-') as temp:
    source = (root / 'Tests/SDKStubs.swift').read_text()
    for file in files:
        code = (root / file).read_text()
        code = re.sub(r'^import (?!Foundation$).*\n', '', code, flags=re.M)
        code = code.replace('@Model\n', '').replace('@Observable\n', '')
        code = code.replace('@Attribute(.unique) ', '')
        # Never touch the application's real shared defaults during host tests.
        code = code.replace('group.com.humblebee.zenlock', 'test.zenlock.' + Path(temp).name)
        source += '\n' + code
    # Exercise the real editor save action without rendering SwiftUI.
    editor = (root / 'ZenLock/Features/Groups/EditGroupView.swift').read_text()
    save_action = editor[editor.index('    private func save() {'):].rsplit('}', 1)[0]
    source += "\nfinal class TestEditor {\n" + """
        let group: BlockGroup
        var draft: GroupDraft
        let modelContext = TestModelContext()
        var retryActivation = false
        var toast: ZenToastData?
        var dismissed = false
        var lockStructure: Bool { group.toShared().isStrictLocked }
        init(_ group: BlockGroup) { self.group = group; draft = GroupDraft(from: group) }
        func dismiss() { dismissed = true }
        func submit() { save() }
    """ + save_action + "\n}\n"
    source += '\n' + (root / 'Tests/UsageTests.swift').read_text()
    path = Path(temp) / 'main.swift'
    path.write_text(source)
    binary = Path(temp) / 'tests'
    subprocess.run(['swiftc', '-module-cache-path', temp + '/cache', str(path), '-o', str(binary)], check=True)
    subprocess.run([str(binary)], check=True)
