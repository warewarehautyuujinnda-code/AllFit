---
name: weekly-plan
description: Use when the user wants to review FitLog weekly performance, set next week's strength or running targets, revise a long-term goal, or explicitly invokes /weekly-plan.
---

# weekly-plan — 週次計画の更新ループ

FitLogの「長期目標→週次計画→実績比較→次週計画」を回す。食事は対象外。決まった計画はコードへ追記するが、外部送信・push・Issue・PR作成の直前には必要な承認を得る。

## 手順

1. PlanSeeds.ktの直近計画と、利用者が書き出した記録JSONを確認する。
2. strengthProgressOfとrunningDistanceOfの定義に沿って先週の計画と実績を比較する。
3. 長期目標を踏まえ、来週の筋トレ種目別reps/setsと週間走行距離を相談して決める。
4. 決定内容を提示し、PlanSeeds.ktを編集してよいかユーザーの明示承認を得る。
5. 最新mainからcodex/plan-<週開始日>ブランチを作る。
6. PlanSeeds.ktへ追記し、既存エントリは変更・削除しない。
7. 単体テストとdebug/releaseビルドを確認し、ローカルコミットする。
8. ユーザー承認後にPRを作成する。マージはしない。

## データ規則

- weekStartは必ず月曜日のyyyy-MM-dd。
- WeeklyStrengthTargetEntity.weekPlanIdは対応するWeeklyPlanEntity.weekStartと同じ値。
- 長期目標を新設・変更した場合だけgoalsへ追記する。
- GoalEntity.createdAtは決定日のyyyy-MM-dd。同日に複数の長期目標を作らない。
- 過去の計画を上書きしない。比較可能性を保つため追記専用とする。

## 守ること

- アプリ内に計画の入力・編集UIを追加しない。
- 食事目標を追加しない。
- mainへ直接コミットまたはpushしない。
- PRを自動マージしない。
- 実績JSONを外部へ送信しない。

## 完了条件

- 来週の計画値がユーザーとの会話で確定している。
- PlanSeeds.ktの関係キーと日付が整合している。
- テストとビルド結果を報告できる。
- 承認された場合のみレビュー待ちPRを開き、マージせず終了する。

## よくある誤り

- 今週の計画を上書きする → 来週分を新規追記する。
- 日曜の日付をweekStartに使う → その週の月曜へ直す。
- 目標reps達成を種目実施判定に使う → 実施判定はその週に1件でも記録があるかで行う。
- 計画相談前にコードを変更する → 先に振り返りと数値合意を済ませる。
