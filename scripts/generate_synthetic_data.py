#!/usr/bin/env python3
"""
FinPilot Deterministic Synthetic Financial Data Generator
Generates 10,000+ realistic financial transactions and companion bank settlement CSVs.
"""

import csv
import random
import argparse
from datetime import datetime, timedelta, timezone

SOURCES = ["PAYGATE-IN", "SWIFT-WIRE", "ACH-CLEARING", "UPI-SWITCH", "STRIPE-CONNECT"]
CURRENCY = "INR"
TYPES = ["PAYMENT", "TRANSFER", "REFUND", "ADJUSTMENT", "FEE"]
STATUSES = ["POSTED", "PENDING", "VALIDATED", "REQUIRES_REVIEW"]

def generate_datasets(num_transactions: int = 10000, seed: int = 42):
    random.seed(seed)
    base_date = datetime(2026, 10, 1, 9, 0, 0, tzinfo=timezone.utc)

    transactions = []
    statement_records = []

    print(f"Generating {num_transactions} synthetic transactions (Random Seed: {seed})...")

    for i in range(1, num_transactions + 1):
        txn_ref = f"TXN-FP-{i:06d}"
        source = random.choice(SOURCES)
        src_acc = f"ACC-{random.randint(1000, 9999)}"
        dst_acc = f"ACC-{random.randint(1000, 9999)}"
        
        # Power-law / log-normal financial distribution
        amount = round(random.lognormvariate(8.0, 1.2), 2)
        if amount < 10.0:
            amount = 10.0

        txn_type = random.choices(TYPES, weights=[0.75, 0.15, 0.05, 0.03, 0.02])[0]
        status = random.choices(STATUSES, weights=[0.80, 0.12, 0.05, 0.03])[0]
        
        # Timestamp drift over 30 days
        date_offset = random.uniform(0, 30)
        txn_time = base_date + timedelta(days=date_offset)

        transactions.append({
            "external_reference": txn_ref,
            "source_system": source,
            "source_account": src_acc,
            "destination_account": dst_acc,
            "amount": amount,
            "currency": CURRENCY,
            "transaction_type": txn_type,
            "status": status,
            "date": txn_time.strftime("%Y-%m-%d"),
            "description": f"Synthetic {txn_type.lower()} transfer from {source}"
        })

        # Companion external bank statement record simulation (80% matched, 5% amount mismatch, 5% date drift, 5% missing)
        r = random.random()
        if r < 0.80:
            # Exact match (Level 1)
            statement_records.append({
                "transaction_reference": txn_ref,
                "amount": amount,
                "currency": CURRENCY,
                "date": txn_time.strftime("%Y-%m-%d"),
                "status": "SETTLED"
            })
        elif r < 0.85:
            # Amount discrepancy (Level 2) - 2.5% fee withholding
            discrepancy_amount = round(amount * 0.975, 2)
            statement_records.append({
                "transaction_reference": txn_ref,
                "amount": discrepancy_amount,
                "currency": CURRENCY,
                "date": txn_time.strftime("%Y-%m-%d"),
                "status": "SETTLED"
            })
        elif r < 0.90:
            # Date drift (Level 3/4) - 3 days delay
            statement_records.append({
                "transaction_reference": txn_ref,
                "amount": amount,
                "currency": CURRENCY,
                "date": (txn_time + timedelta(days=3)).strftime("%Y-%m-%d"),
                "status": "SETTLED"
            })
        elif r < 0.93:
            # Duplicate bank row
            statement_records.append({
                "transaction_reference": txn_ref,
                "amount": amount,
                "currency": CURRENCY,
                "date": txn_time.strftime("%Y-%m-%d"),
                "status": "SETTLED"
            })
            statement_records.append({
                "transaction_reference": txn_ref,
                "amount": amount,
                "currency": CURRENCY,
                "date": txn_time.strftime("%Y-%m-%d"),
                "status": "SETTLED"
            })
        # Remainder (7%): Missing from external statement

    # Add 150 phantom external records missing internally
    for j in range(1, 151):
        phantom_ref = f"EXT-UNMATCHED-{j:04d}"
        phantom_amount = round(random.uniform(500, 25000), 2)
        statement_records.append({
            "transaction_reference": phantom_ref,
            "amount": phantom_amount,
            "currency": CURRENCY,
            "date": (base_date + timedelta(days=random.uniform(0, 30))).strftime("%Y-%m-%d"),
            "status": "SETTLED"
        })

    # Write Transactions CSV
    with open("transactions_10k.csv", "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=transactions[0].keys())
        writer.writeheader()
        writer.writerows(transactions)

    # Write External Bank Statement CSV
    with open("bank_statement_10k.csv", "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=statement_records[0].keys())
        writer.writeheader()
        writer.writerows(statement_records)

    print(f"Generated transactions_10k.csv with {len(transactions)} rows.")
    print(f"Generated bank_statement_10k.csv with {len(statement_records)} rows.")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--count", type=int, default=10000, help="Number of transactions to generate")
    parser.add_argument("--seed", type=int, default=42, help="Deterministic random seed")
    args = parser.parse_args()
    generate_datasets(args.count, args.seed)
