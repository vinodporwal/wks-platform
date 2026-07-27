"""
Parse C2_CPP log and extract power demand/supply breakdown.
"""

import re
import sys

log_path = r"c:\Users\shrik\Desktop\Project\fork repo\development\New\JMD new python script\wks-platform\apps\python\JMD\logs\C2_CPP\C2_CPP_2026_04_20260723_102009.log"

def main():
    with open(log_path, "r", encoding="utf-8") as f:
        lines = f.readlines()

    # Find final summary section
    in_summary = False
    for i, line in enumerate(lines):
        if "U4U ITERATION LOOP — FINAL SUMMARY" in line:
            in_summary = True
            start = i
            break

    if not in_summary:
        print("Final summary not found")
        return

    # Print final summary power balance block
    print("=" * 80)
    print("FINAL POWER SUMMARY")
    print("=" * 80)
    for line in lines[start:start+25]:
        print(line.rstrip())

    # Find Power asset dispatch summary
    print("\n" + "=" * 80)
    print("POWER ASSET DISPATCH")
    print("=" * 80)
    in_dispatch = False
    for i, line in enumerate(lines):
        if "Power Asset Dispatch Summary:" in line:
            in_dispatch = True
            start = i
            continue
        if in_dispatch:
            print(line.rstrip())
            if "Total Plant Capacity" in line and "Current Generation" in lines[i+1]:
                print(lines[i+1].rstrip())
                break

    # Find demand block with Power_Dis
    print("\n" + "=" * 80)
    print("DEMAND TABLE (Power_Dis row)")
    print("=" * 80)
    for i, line in enumerate(lines):
        if "Power_Dis" in line and "DEMAND" in lines[max(0, i-5):i]:
            print(line.rstrip())

    # Find generation utility comparison summary
    print("\n" + "=" * 80)
    print("GENERATION UTILITY COMPARISON SUMMARY")
    print("=" * 80)
    in_comp = False
    for i, line in enumerate(lines):
        if "GENERATION UTILITY COMPARISON SUMMARY" in line:
            in_comp = True
            start = i
            for j in range(i, min(i+15, len(lines))):
                print(lines[j].rstrip())
            break


if __name__ == "__main__":
    main()
