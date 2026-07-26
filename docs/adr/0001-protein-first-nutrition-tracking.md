# 0001. Protein-first nutrition tracking

Date: 2026-07-26

## Status

Accepted

## Context

The meal logger required calories, protein, carbohydrates, and fat even though daily use only needed protein progress and a creatine reminder. Calorie guidance remains useful for body-weight planning, but consumed calories are not logged. Reusing the old meal and generic-supplement schema would leave obsolete fields, templates, and reminder behavior embedded in the active domain.

Existing on-device and backup data must remain importable, per-user isolation must remain intact, and historical protein progress must not be reinterpreted when a goal weight or manual target changes.

## Decision

We will model nutrition as dated whole-gram protein entries, dated protein-target snapshots, one per-user creatine configuration, and one reversible creatine check per date.

We will calculate calorie guidance and protein targets independently. Calorie guidance keeps the existing Mifflin-St Jeor and trend-adjustment behavior; the protein target uses 2.0 g/kg goal body weight unless manually overridden.

We will migrate Room v7 to v8 non-destructively. Positive protein values from legacy food entries become protein entries, seeded creatine data becomes creatine configuration and checks, and obsolete meal templates and non-creatine supplements are discarded. Backup schema v8 will retain import compatibility with v1 through v7 using the same conversion rules.

## Consequences

Daily logging becomes faster and the active data model matches what the app displays. Historical target snapshots make trend comparisons stable, and creatine reminders can suppress notifications after completion.

The migration and backup importer must carry dedicated legacy conversion code. Carb, fat, meal-template, and non-creatine supplement data no longer survive the upgrade, and historical protein rows have no target marker because earlier versions did not capture one.
