---
name: kaizen
description: Turns a piece of feedback about the FitLog app (a typed complaint, a spoken note, or an annotated screenshot from the app's own in-app feedback capture) into a GitHub Issue, a dedicated branch, an implemented fix, and an open pull request — the standing improvement loop for this repo. Use this whenever the user reports something annoying, broken, missing, or worth improving in FitLog, even if they don't type "/kaizen" explicitly — e.g. "the weight screen's keyboard doesn't show decimals", "add a clear button to the strength history", or a shared screenshot with red marks on it. Always invoke explicitly on /kaizen too.
---

# Kaizen — FitLogの改善ループ

FitLog を改善するときは、思いつきで直接コードを触るのではなく、必ずこの4ステップを1周として回す。
「小さく、頻繁に、記録を残しながら直していく」ための型そのものが目的であり、機能そのもの以上に
この型を回せることに価値がある。

## なぜこの形なのか

- **Issueから始める理由**: 何を直すのかを先に言葉にしておくと、後から「結局何を直したんだっけ」を
  追える。フィードバックが曖昧でも、ここで止まらず一番自然な解釈で進めてよい。確認待ちで手が止まる
  ことの方が、多少の解釈のズレより損失が大きい。
- **ブランチを切る理由**: mainは常に動く状態を保つ。ブランチ上でのミスはいくらでもやり直しがきく。
- **実装まで確認なしで進めてよい理由**: ブランチ上のコードは壊れてもいいものとして扱ってよい。ここで
  逐一確認を挟むと、「ちょっとした不満を1日1回投げるだけで、気づいたら直っている」という本来やりたい
  体験そのものが壊れてしまう。
- **マージだけは人間が判断する理由**: これだけは譲れない一線。ブランチ上の実験はいくらでもやり直せる
  が、mainにマージされたものはそのまま本人のスマホで動く実運用中のアプリになる。体重・食事・トレーニ
  ングという実データを扱う以上、「動くようになったつもり」のまま自動でマージ・配信されるのは避ける。
  だからこのスキルの最終成果物は常に「レビュー待ちのPR」であり、「マージ済み」では絶対にない。

## 手順

### 1. Issueとして明文化する

渡された内容(文章、および添付があればスクリーンショット)から、以下を含むGitHub Issueを作成する。

- タイトル: どの画面で何が起きているかが具体的にわかるように
  (例: 「体重画面: 数値キーボードが小数点に対応していない」)
- 本文: 何が問題か / どうなってほしいか。スクリーンショットに赤線の書き込みがあれば、その位置が何を
  指しているかを言葉でも書き添える。

内容が曖昧なら、最も自然な解釈で進める。ここでユーザーの確認は待たない。

### 2. Issueに対応するブランチを切る

最新のmainから分岐する。命名はこのリポジトリの既存の流儀に合わせて
`issue-<Issue番号>-<内容を表す短い英語スラッグ>`(例: `issue-4-running-record`)。

### 3. そのブランチ上で改修する

- そのIssueの範囲に留める。ついでの別改善は混ぜない。
- 実装後、既存のユニットテスト(`app/src/test`)やビルドなど、その場で確認できる手段があれば確認する。
  ビルド環境が無い(例: Google Mavenへの到達がネットワークポリシーでブロックされている)場合は、その旨
  を最終報告とPR本文に明記した上で先に進めてよい。確認できないこと自体は作業を止める理由にはしない。
- Issue作成・ブランチ作成・実装・コミットまでの一連の操作は、人間の承認を待たずに進めてよい。

### 4. プルリクエストを作成する

- 対応するIssueを閉じる形でリンクする(例: `Closes #<番号>`)。
- 何を変えたか、何を確認できたか(できなかった場合はそれも)を書く。
- **ここでマージはしない。** PRを開いた時点でこのスキルの仕事は完了。マージするかどうかは必ず人間が
  判断する。

## 守ること

- mainへの直接コミット・直接pushはしない。
- PRを自動でマージしない。承認・マージは常に人間が行う。
- 1回の呼び出しにつき、Issue 1件・ブランチ1本・PR1件を基本とする。明らかに独立した複数の不満が同時に
  来た場合は、無理に1本へ混ぜ込まず、Issue・ブランチ・PRをそれぞれ分けて複数周する。
