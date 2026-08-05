#!/usr/bin/env python3
"""Render a forecast.py JSON dump as a single self-contained HTML report.

Companion to forecast.py:
  1. python3 forecast.py --done … --open … --json-out forecast.json
  2. python3 forecast-html.py forecast.json report.html

Takes only file-path args and never touches the network, so it's safe to run
anywhere and prompt-free when allow-listed by exact path.

Charts are plain HTML/CSS bars (no JS, no external assets) so the file stays a
single static document safe to open anywhere or attach to chat. Per the dataviz
method: horizontal magnitude bars with rounded data-ends anchored to a baseline,
one primary hue for magnitude, a reserved status palette (breach/aging/healthy)
that ALWAYS ships with an icon + text label + sort order (never color alone), and
native hover tooltips via SVG/element titles.

To re-skin for your brand, swap the C_* palette constants and the HEADLINE/BODY
fonts below (and drop in a logo in the header if you want one). The default
palette is validated for contrast; keep an eye on it if you change the hues.

Usage:  python3 forecast-html.py [in_json] [out_html]
        (defaults: /tmp/forecast.json → /tmp/forecast_report.html)
"""

import html
import json
import sys
from pathlib import Path

IN = Path(sys.argv[1] if len(sys.argv) > 1 else "/tmp/forecast.json")
OUT = Path(sys.argv[2] if len(sys.argv) > 2 else "/tmp/forecast_report.html")

# --- Report palette (swap these hexes for your brand; contrast-validated) ---
C_RED = "#E10321"       # Core Red
C_BLUE = "#0B4CDB"      # Core Blue — primary magnitude hue for bars
C_TEAL = "#5EDEFC"      # Core Teal
C_TAN = "#FBF6F0"       # Core Tan — page background
C_DARK = "#041531"      # Core Dark — primary text
C_TAN_SH1 = "#F2ECE3"   # Tan Shade 1 — note bg, bar tracks
C_TEAL_T2 = "#DBF8FF"   # Teal Tint 2 — pill bg (AAA 13.75 w/ Blue Shade 3)
C_BLUE_SH3 = "#06244F"  # Blue Shade 3 — secondary text, pill text
C_GRAY_T2 = "#D3D8E5"   # Gray Tint 2 — hairlines

# Reserved status palette (validated: contrast >=3:1 on white & tan; the red/amber
# CVD closeness is in the 8-12 floor band, legal here because every status mark
# ships an icon + text label + sort order — secondary encoding, never color alone).
ST_BREACH = "#C21807"    # critical — over the 85% SLE
ST_WARN = "#B8730E"      # warning — aging (between typical and SLE)
ST_HEALTHY = "#1F8A4C"   # good — within the typical line
BAND = {
    "breach":  {"color": ST_BREACH,  "icon": "\U0001F534", "label": "over SLE"},
    "warn":    {"color": ST_WARN,    "icon": "\U0001F7E1", "label": "aging"},
    "healthy": {"color": ST_HEALTHY, "icon": "\U0001F7E2", "label": "healthy"},
    "none":    {"color": C_BLUE,    "icon": "•",     "label": ""},
}

HEADLINE_FONT = "'Degular Display', Degular, 'Avenir Next', 'Helvetica Neue', Arial, sans-serif"
BODY_FONT = "'Aktiv Grotesk', 'Helvetica Neue', Helvetica, Arial, sans-serif"

# Optional brand logo, inlined so the report stays a single self-contained file.
# Leave empty for the neutral template; set to an inline <svg>…</svg> string to
# show a logo in the header.
LOGO_SVG = ""


def esc(v):
    return html.escape(str(v)) if v is not None else "—"


def card(name, value, pill, detail_lines, accent=None):
    detail = "<br>".join(d for d in detail_lines if d)
    pill_html = f'<span class="pill">{esc(pill)}</span>' if pill else ""
    style = f' style="border-left:4px solid {accent}"' if accent else ""
    return (f'<div class="card"{style}><div class="name">{esc(name)}</div>'
            f'<div class="value">{esc(value)}</div>{pill_html}'
            f'<div class="detail">{detail}</div></div>')


def dual_card(name, v50, v85, detail_lines, accent=None):
    """Card that gives BOTH confidence figures equal billing: 50% for the
    internal/team read, 85% (accent) for the stakeholder/commit read."""
    detail = "<br>".join(d for d in detail_lines if d)
    style = f' style="border-left:4px solid {accent}"' if accent else ""
    return (f'<div class="card"{style}><div class="name">{esc(name)}</div>'
            f'<div class="dual">'
            f'<div class="dv"><div class="dvlab">50% · team</div>'
            f'<div class="dvnum">{esc(v50)}</div></div>'
            f'<div class="dv"><div class="dvlab">85% · stakeholder</div>'
            f'<div class="dvnum accent">{esc(v85)}</div></div>'
            f'</div>'
            f'<div class="detail">{detail}</div></div>')


def hbar(rows, axis_max, unit="", axis_lines=None, clip=None):
    """Horizontal magnitude bars. rows = [(label, value, value_label, color, tip)]
    or 6-tuples with a trailing href — when present the row label becomes a link.
    axis_lines = [(value, label)] reference markers. clip caps the axis (over-cap
    bars render full-width with a real-value label + a clipped edge)."""
    axis_max = max(axis_max, 1)
    marks = ""
    for val, lab in (axis_lines or []):
        pos = min(100, 100 * val / axis_max)
        marks += (f'<div class="axline" style="left:{pos:.1f}%" '
                  f'title="{esc(lab)}"><span>{esc(lab)}</span></div>')
    region = f'<div class="trackregion">{marks}</div>' if marks else ""
    body = ""
    for row in rows:
        label, value, vlabel, color, tip = row[:5]
        href = row[5] if len(row) > 5 else None
        label_html = (f'<a href="{esc(href)}" target="_blank" rel="noopener">{esc(label)}</a>'
                      if href else esc(label))
        capped = clip is not None and value > clip
        shown = min(value, axis_max)
        w = 100 * shown / axis_max
        edge = "bar-clip" if capped else ""
        body += (
            f'<div class="brow"><div class="blabel" title="{esc(tip)}">{label_html}</div>'
            f'<div class="btrack">'
            f'<div class="bfill {edge}" style="width:{w:.1f}%;background:{color}" '
            f'title="{esc(tip)}"></div></div>'
            f'<div class="bval">{esc(vlabel)}</div></div>')
    return f'<div class="chart">{region}{body}</div>'


def col_chart(values, labels, vmax, mean=None, outliers=None):
    """Vertical throughput columns, one per week, height-encoded. `outliers` is a
    set of 0-based indices to flag (amber bar + ▲ mark + title)."""
    vmax = max(vmax, 1)
    outliers = outliers or set()
    cols = ""
    for idx, (v, lab) in enumerate(zip(values, labels)):
        h = 100 * v / vmax
        is_out = idx in outliers
        barcls = "cbar out" if is_out else "cbar"
        ttl = f'{lab}: {v}' + (" — statistical outlier" if is_out else "")
        mark = '<span class="omark" title="statistical outlier">▲</span>' if is_out else ""
        cols += (f'<div class="col" title="{esc(ttl)}">'
                 f'{mark}<span class="cval">{v}</span>'
                 f'<div class="{barcls}" style="height:{h:.0f}%"></div>'
                 f'<span class="clab">{esc(lab)}</span></div>')
    mean_line = ""
    if mean is not None:
        pos = 100 * mean / vmax
        mean_line = (f'<div class="meanline" style="bottom:calc({pos:.0f}% + 18px)" '
                     f'title="mean {mean}/wk"><span>mean {mean}</span></div>')
    return f'<div class="colchart">{mean_line}{cols}</div>'


def main():
    data = json.loads(IN.read_text())
    m = data["meta"]
    series = data["series"]
    team = series[0]
    comps = series[1:]
    cycle = data.get("cycle")
    wip = data.get("wip", [])
    bands = data.get("wip_bands", {})
    policy = m.get("outlier_policy", "keep")

    def outlier_idx(s):
        return {o["wk"] - 1 for o in s.get("outliers", [])}

    def outlier_caption(s):
        outs, note = s.get("outliers", []), s.get("outlier_note")
        if not outs and not note:
            return ""
        parts = []
        if outs:
            marks = ", ".join(f'w{o["wk"]} = {o["value"]} (robust z {o["z"]:+.1f})' for o in outs)
            if policy == "keep":
                parts.append(f'▲ {marks} flagged as a statistical outlier, still '
                             'in the forecast sample (policy: <b>keep</b>). A single '
                             'artifact week — e.g. a bulk ticket close — biases the '
                             'forecast optimistic; re-run with <code>--outliers '
                             'drop</code> or <code>winsorize</code> to adapt.')
            else:
                parts.append(f'▲ {marks} flagged as a statistical outlier.')
        if note:
            parts.append(f'Applied: {esc(note)}.')
        return f'<div class="note">{" ".join(parts)}</div>'

    # ---------- Overview panel ----------
    def build_overview():
        no_forecast = team.get("q1") is None or team.get("q2") is None
        cards = [] if no_forecast else [
            dual_card("Clear the open queue",
                      f"{team['q1']['w50']}w", f"{team['q1']['w85']}w",
                      [f"{team['backlog']} items · 85% by <b>{esc(team['q1']['date85'])}</b>",
                       f"95% floor {team['q1']['w95']}w"],
                      accent=C_BLUE),
            dual_card(f"Ship by {esc(m['by_date'])}",
                      f"≥ {team['q2']['c50']}", f"≥ {team['q2']['c85']}",
                      [f"{m['n_weeks']} wks out · 95% floor ≥ {team['q2']['c95']}",
                       "at-least-N items, team-wide"],
                      accent=C_TEAL),
        ]
        if cycle:
            cards.append(card("Cycle-time SLE", f"{cycle['p85']:.0f}d", "85% per-item",
                              [f"typical {cycle['p50']:.0f}d · worst {cycle['p95']:.0f}d",
                               f"{esc(m['basis'])} · n={cycle['n']}"],
                              accent=C_BLUE_SH3))
        cards.append(card("Aging WIP over SLE", bands.get("breach", 0),
                          "pull-first", [f"{bands.get('warn',0)} aging · "
                          f"{bands.get('healthy',0)} healthy",
                          f"of {bands.get('total',0)} in flight"],
                          accent=ST_BREACH))
        cardhtml = f'<div class="cards">{"".join(cards)}</div>'

        if no_forecast:
            note = ('<div class="note">No complete throughput weeks fall inside this '
                     f'window ({esc(m.get("window",""))}) — the delivery forecast (Q1/Q2) '
                     'needs at least one full calendar week of Done items and can\'t be '
                     'computed here. Showing cycle-time SLE and aging WIP only, below.</div>')
            return cardhtml + note

        # Team throughput columns
        wk_labels = [f"w{i+1}" for i in range(len(team["sample"]))]
        tp = ('<h2>Weekly throughput — team</h2>'
              '<div class="sub">items reaching Done per complete week, oldest → newest '
              f'({m["sample_weeks"]} weeks)</div>'
              + col_chart(team["sample"], wk_labels, max(team["sample"]),
                          mean=team["mean"], outliers=outlier_idx(team))
              + outlier_caption(team))

        # Q1 — weeks to clear, all series
        q1_max = max(s["q1"]["w95"] for s in series)
        q1_rows = [(s["name"], s["q1"]["w85"], f'{s["q1"]["w85"]}w',
                    C_BLUE if s is team else C_BLUE_SH3,
                    f'{s["name"]}: 50% {s["q1"]["w50"]}w, 85% {s["q1"]["w85"]}w '
                    f'({esc(s["q1"]["date85"])}), 95% {s["q1"]["w95"]}w')
                   for s in series]
        q1 = ('<h2>Q1 · How long to clear each queue</h2>'
              '<div class="sub">bar = 85% confidence (commit-safe); hover for the 50/85/95 range + date</div>'
              + hbar(q1_rows, q1_max, unit="w"))

        # Q2 — items by date
        q2_max = max(s["q2"]["c50"] for s in series) or 1
        q2_rows = [(s["name"], s["q2"]["c85"], f'≥ {s["q2"]["c85"]}',
                    C_BLUE if s is team else C_BLUE_SH3,
                    f'{s["name"]}: ≥95% {s["q2"]["c95"]}, ≥85% {s["q2"]["c85"]}, ≥50% {s["q2"]["c50"]}')
                   for s in series]
        q2 = (f'<h2>Q2 · How many done by {esc(m["by_date"])}</h2>'
              '<div class="sub">bar = at least this many at 85% confidence</div>'
              + hbar(q2_rows, q2_max))

        note = ('<div class="note">Component forecasts are <b>independent</b> — the '
                'TEAM line is not the sum of the component lines (variances don’t add). '
                'Trust TEAM for whole-team promises; use component lines to spot bottlenecks '
                'and lopsided queues.</div>')
        return cardhtml + tp + q1 + q2 + note

    # ---------- Cycle-time panel ----------
    def build_cycle():
        if not cycle:
            return '<div class="note">No cycle-time sample in this run (no `created` field pulled).</div>'
        cards = [card("50% typical", f"{cycle['p50']:.0f}d", None, ["half of items finish within"]),
                 card("85% SLE", f"{cycle['p85']:.0f}d", "commitment",
                      ["the per-item promise — quote this"], accent=C_BLUE),
                 card("95% worst", f"{cycle['p95']:.0f}d", None,
                      [f"longest {cycle['max']:.0f}d · n={cycle['n']}"])]
        hist = cycle["hist"]
        hmax = max((b["count"] for b in hist), default=1)
        rows = [(b["label"] + "d", b["count"], str(b["count"]), C_BLUE,
                 f'{b["count"]} items resolved in {b["label"]} days')
                for b in hist]
        chart = ('<h2>Cycle-time distribution</h2>'
                 f'<div class="sub">{esc(m["basis"])}, calendar days — where the '
                 'time actually goes</div>' + hbar(rows, hmax))
        note = ('<div class="note"><b>Read with care:</b> this is '
                f'<b>{esc(m["basis"])}</b>, which includes backlog wait — a thick '
                '30–90d shoulder inflates the 85% line. The tighter, standard measure '
                'is <b>started→Done</b> (active time via the changelog); re-run the '
                'forecast with <code>--changelog</code> for the true SLE.</div>')
        return f'<div class="cards">{"".join(cards)}</div>{chart}{note}'

    # ---------- Aging WIP panels (split: actionable ≤180d vs parked >180d) ------
    PARK = 180  # days: above this an item is "parked" (almost certainly stale)
    active_wip = [w for w in wip if w["age"] <= PARK]
    parked_wip = [w for w in wip if w["age"] > PARK]

    browse = m.get("jira_browse_base")  # e.g. https://you.atlassian.net/browse

    def _wip_rows(items):
        rows = []
        for w in items:
            b = BAND.get(w["band"], BAND["none"])
            tip = (f'{w["key"]} · {w["status"]} · {w["age"]} {esc(m["age_label"])}'
                   f' — {b["label"] or "in flight"}')
            href = f'{browse}/{w["key"]}' if browse else None
            rows.append((f'{b["icon"]} {w["key"]}', w["age"], f'{w["age"]}d',
                         b["color"], tip, href))
        return rows

    _legend = ('<div class="legend">'
               f'<span><i style="background:{ST_BREACH}"></i>\U0001F534 over SLE — pull now</span>'
               f'<span><i style="background:{ST_WARN}"></i>\U0001F7E1 aging — watch</span>'
               f'<span><i style="background:{ST_HEALTHY}"></i>\U0001F7E2 within typical</span>'
               '</div>')

    def build_wip_active():
        if not wip:
            return '<div class="note">No in-flight items in the aging-WIP statuses.</div>'
        if not active_wip:
            return ('<div class="note">Every in-flight item has been open &gt;'
                    f'{PARK}d — see the <b>Parked WIP</b> tab.</div>')
        sle85 = cycle["p85"] if cycle else None
        sle50 = cycle["p50"] if cycle else None
        # axis just past the oldest active item (no clipping on this page), with
        # enough room that the SLE line never sits at the far edge.
        axis = max(int(max(w["age"] for w in active_wip) * 1.08),
                   int((sle85 or 1) * 1.25), 1)
        axis_lines = []
        if sle50 is not None:
            axis_lines.append((sle50, f'typical {sle50:.0f}d'))
        if sle85 is not None:
            axis_lines.append((sle85, f'85% SLE {sle85:.0f}d'))
        b = sum(1 for w in active_wip if w["band"] == "breach")
        wn = sum(1 for w in active_wip if w["band"] == "warn")
        h = sum(1 for w in active_wip if w["band"] == "healthy")
        summary = (f'<div class="sub">{b} over SLE · {wn} aging · {h} healthy '
                   f'({len(active_wip)} in flight ≤{PARK}d). '
                   + (f'<b>{len(parked_wip)}</b> older items (&gt;{PARK}d) are on the '
                      '<b>Parked WIP</b> tab.' if parked_wip else '')
                   + ' Ticked lines are the SLE bands.</div>')
        return ('<h2>Q4 · Aging work in progress — pull these before starting new work</h2>'
                + summary + _legend + hbar(_wip_rows(active_wip), axis, unit="d",
                                           axis_lines=axis_lines))

    def build_wip_parked():
        if not parked_wip:
            return f'<div class="note">No items parked beyond {PARK} days. \U0001F389</div>'
        oldest = max(w["age"] for w in parked_wip)
        sle85 = cycle["p85"] if cycle else None
        summary = (f'<div class="sub">{len(parked_wip)} items have been in flight '
                   f'<b>over {PARK} days</b>'
                   + (f' — far past the {sle85:.0f}d SLE' if sle85 else '')
                   + '. These are almost certainly stale: confirm they’re still real '
                   f'work, or close/split them. Bars scaled to the oldest ({oldest}d).</div>')
        return ('<h2>Parked WIP · in flight &gt; ' + str(PARK) + ' days</h2>'
                + summary + _legend + hbar(_wip_rows(parked_wip), int(oldest * 1.02), unit="d"))

    # ---------- Per-component panels ----------
    def build_component(s):
        cards = [
            card("Open queue", s["backlog"], None, ["items awaiting delivery"]),
            dual_card("Clear it", f"{s['q1']['w50']}w", f"{s['q1']['w85']}w",
                      [f"85% by <b>{esc(s['q1']['date85'])}</b>",
                       f"95% floor {s['q1']['w95']}w"],
                      accent=C_BLUE),
            dual_card(f"By {esc(m['by_date'])}",
                      f"≥ {s['q2']['c50']}", f"≥ {s['q2']['c85']}",
                      [f"95% floor ≥ {s['q2']['c95']}"]),
            card("Weekly rate", f"{s['mean']}/wk", None,
                 [f"range {s['min']}–{s['max']} · {m['sample_weeks']} wks"]),
        ]
        wk_labels = [f"w{i+1}" for i in range(len(s["sample"]))]
        chart = ('<h2>Weekly throughput</h2>'
                 + col_chart(s["sample"], wk_labels, max(max(s["sample"]), 1),
                             mean=s["mean"], outliers=outlier_idx(s))
                 + outlier_caption(s))
        return (f'<h2 class="plat-title">{esc(s["name"])}</h2>'
                f'<div class="cards">{"".join(cards)}</div>{chart}')

    # ---------- Assemble tabs ----------
    panels = [("Overview", build_overview())]
    if cycle:
        panels.append(("Cycle Time", build_cycle()))
    panels.append(("Aging WIP", build_wip_active()))
    if parked_wip:
        panels.append(("Parked WIP", build_wip_parked()))
    for s in comps:
        panels.append((s["name"], build_component(s)))

    # Every tab references throughput/queue data, so surface the Epic-exclusion
    # scope on each one (assembled here so future tabs inherit it automatically).
    epics_chip = ('<div class="scopechip" title="Epic issues are placeholders, '
                  'not deliverable flow — excluded from every query and figure">'
                  '⊘ Epics excluded from all figures</div>'
                  if m.get("epics_excluded") else '')

    radios = "".join(f'<input type="radio" name="tab" id="t{i}"{" checked" if i == 0 else ""}>'
                     for i in range(len(panels)))
    labels = "".join(f'<label for="t{i}">{esc(name)}</label>' for i, (name, _) in enumerate(panels))
    panel_divs = "".join(f'<div class="panel pan{i}">{epics_chip}{body}</div>'
                         for i, (_, body) in enumerate(panels))
    tab_rules = "".join(
        f'#t{i}:checked~.layout .panels .pan{i}{{display:block;}}'
        f'#t{i}:checked~.layout .sidebar label[for=t{i}]{{background:{C_BLUE};color:#fff;border-color:{C_BLUE};}}'
        for i in range(len(panels)))

    CSS = f"""
body {{ font-family: {BODY_FONT}; margin: 0; background: {C_TAN}; color: {C_DARK}; }}
input[name="tab"] {{ position: absolute; opacity: 0; pointer-events: none; }}
.layout {{ display: flex; max-width: 1080px; margin: 0 auto; gap: 20px; padding: 24px 16px 48px; }}
.sidebar {{ flex: 0 0 170px; position: sticky; top: 16px; align-self: flex-start;
           display: flex; flex-direction: column; gap: 8px; }}
.side-head {{ font-family: {HEADLINE_FONT}; font-weight: 700; color: {C_BLUE_SH3};
             font-size: 13px; padding: 4px 2px 8px; }}
.sidebar label {{ display: block; background: #FFFFFF; border: 1px solid {C_GRAY_T2};
                 border-radius: 10px; padding: 9px 12px; font-size: 14px; font-weight: 600;
                 color: {C_BLUE_SH3}; cursor: pointer; }}
.sidebar label:hover {{ border-color: {C_BLUE}; }}
.panels {{ flex: 1; min-width: 0; }}
.panel {{ display: none; }}
{tab_rules}
header {{ border-bottom: 4px solid {C_RED}; padding: 0 0 12px; margin-bottom: 20px;
         display: flex; justify-content: space-between; align-items: center; gap: 16px; }}
.logo svg {{ width: 96px; height: auto; display: block; }}
h1 {{ font-family: {HEADLINE_FONT}; font-size: 26px; font-weight: 700; margin: 0 0 4px; color: {C_BLUE}; }}
h2 {{ font-family: {HEADLINE_FONT}; font-size: 17px; font-weight: 700; margin: 26px 0 4px; color: {C_BLUE_SH3}; }}
h2.plat-title {{ margin-top: 0; font-size: 21px; color: {C_BLUE}; }}
.sub {{ color: {C_BLUE_SH3}; font-size: 12px; margin-bottom: 10px; }}
.cards {{ display: flex; flex-wrap: wrap; gap: 12px; }}
.card {{ flex: 1 1 200px; background: #FFFFFF; border-radius: 12px; padding: 14px 16px;
        border: 1px solid {C_GRAY_T2}; }}
.card .name {{ font-family: {HEADLINE_FONT}; font-size: 13px; font-weight: 700; color: {C_BLUE}; }}
.card .value {{ font-size: 30px; font-weight: 800; margin: 4px 0 2px; color: {C_DARK}; }}
.card .detail {{ font-size: 12px; color: {C_BLUE_SH3}; line-height: 1.5; }}
.dual {{ display: flex; gap: 12px; margin: 6px 0 4px; }}
.dv {{ flex: 1 1 0; min-width: 0; }}
.dvlab {{ font-size: 10px; font-weight: 700; color: {C_BLUE_SH3}; white-space: nowrap; letter-spacing: .2px; }}
.dvnum {{ font-size: 25px; font-weight: 800; line-height: 1.1; color: {C_DARK}; }}
.dvnum.accent {{ color: {C_BLUE}; }}
.dv:last-child {{ border-left: 1px solid {C_GRAY_T2}; padding-left: 12px; }}
.pill {{ display: inline-block; font-size: 11px; font-weight: 700; border-radius: 999px;
        padding: 2px 10px; background: {C_TEAL_T2}; color: {C_BLUE_SH3}; margin-top: 4px; }}
.scopechip {{ display: inline-block; font-size: 11px; font-weight: 700; border-radius: 999px;
        padding: 3px 12px; margin: 0 0 14px; background: {C_TAN_SH1}; color: {C_BLUE_SH3};
        border: 1px solid {C_GRAY_T2}; }}
.note {{ background: {C_TAN_SH1}; border-left: 4px solid {C_RED}; padding: 8px 12px;
        font-size: 13px; border-radius: 0 8px 8px 0; margin: 14px 0; color: {C_DARK}; line-height: 1.5; }}
/* horizontal magnitude bars: [ label gutter | track | value gutter ] */
.chart {{ position: relative; background: #FFFFFF; border: 1px solid {C_GRAY_T2};
         border-radius: 10px; padding: 28px 14px 16px; }}  /* top strip = axis-label headroom */
.brow {{ display: flex; align-items: center; gap: 10px; margin: 4px 0; }}
.blabel {{ flex: 0 0 118px; font-size: 12px; font-weight: 600; color: {C_DARK};
          text-align: left; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }}
.blabel a {{ color: {C_BLUE}; text-decoration: underline; }}
.blabel a:hover {{ color: {C_BLUE_SH3}; }}
.btrack {{ position: relative; flex: 1; background: {C_TAN_SH1}; border-radius: 5px; height: 16px; }}
.bfill {{ height: 100%; border-radius: 0 5px 5px 0; min-width: 2px; }}
.bfill.bar-clip {{ border-radius: 0; -webkit-mask-image: linear-gradient(90deg,#000 82%,transparent 99%);
                  mask-image: linear-gradient(90deg,#000 82%,transparent 99%); }}
.bval {{ flex: 0 0 46px; font-size: 11px; font-weight: 700; color: {C_BLUE_SH3};
        white-space: nowrap; }}
/* axis reference lines live in an overlay inset to exactly match the track column
   (left = padding14 + label118 + gap10 = 142; right = padding14 + value46 + gap10 = 70);
   top/bottom match chart padding so the dashed lines span the bars only, and each
   label floats up into the top strip above the first bar. */
.trackregion {{ position: absolute; top: 28px; bottom: 16px; left: 142px; right: 70px;
               pointer-events: none; }}
.axline {{ position: absolute; top: 0; bottom: 0; width: 0; border-left: 2px dashed {C_RED}; }}
.axline span {{ position: absolute; top: -18px; left: 4px; font-size: 9px; font-weight: 700;
               color: {C_RED}; white-space: nowrap; background: #FFFFFF; padding: 0 2px; }}
/* throughput columns */
.colchart {{ position: relative; display: flex; align-items: flex-end; gap: 6px; height: 150px;
            background: #FFFFFF; border: 1px solid {C_GRAY_T2}; border-radius: 10px;
            padding: 14px 14px 6px; }}
.col {{ flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: flex-end;
       height: 100%; }}
.col .cbar {{ width: 60%; max-width: 34px; background: {C_BLUE}; border-radius: 4px 4px 0 0; min-height: 2px; }}
.col .cbar.out {{ background: {ST_WARN}; }}
.col .omark {{ font-size: 10px; font-weight: 700; color: {ST_WARN}; line-height: 1; margin-bottom: 1px; }}
.col .cval {{ font-size: 10px; font-weight: 700; color: {C_BLUE_SH3}; margin-bottom: 2px; }}
.col .clab {{ font-size: 9px; color: {C_BLUE_SH3}; margin-top: 3px; }}
.meanline {{ position: absolute; left: 14px; right: 14px; border-top: 2px dashed {C_RED}; }}
.meanline span {{ position: absolute; right: 0; top: -12px; font-size: 9px; font-weight: 700; color: {C_RED}; }}
.legend {{ display: flex; flex-wrap: wrap; gap: 16px; font-size: 12px; color: {C_DARK}; margin: 4px 0 10px; }}
.legend i {{ display: inline-block; width: 11px; height: 11px; border-radius: 3px; margin-right: 5px;
            vertical-align: -1px; }}
footer {{ margin-top: 32px; font-size: 11px; color: {C_BLUE_SH3}; line-height: 1.6; }}
@media (max-width: 720px) {{
  .layout {{ flex-direction: column; }}
  .sidebar {{ position: static; flex-direction: row; flex-wrap: wrap; }}
  .blabel {{ flex-basis: 84px; }}
  .trackregion {{ left: 108px; right: 70px; }}
}}
"""

    # Report labels are driven by --project. When it's absent, don't double the
    # word or dangle a "·" — fall back to a clean "Delivery Forecast".
    proj = m.get('project') or ""
    ppfx = f"{esc(proj)} " if proj else ""            # "DQLS " or ""
    pside = f"Forecast · {esc(proj)}" if proj else "Forecast"
    OUT.write_text(f"""<!DOCTYPE html>
<html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{ppfx}Forecast — {esc(m['today'])}</title>
<style>{CSS}</style></head><body>
{radios}
<div class="layout">
<aside class="sidebar"><div class="side-head">{pside}</div>{labels}</aside>
<main class="panels">
<header><div class="title"><h1>{ppfx}Delivery Forecast</h1>
<div class="sub">throughput-based (no story points) · {esc(m['sample_weeks'])} weeks ·
window: {esc(m['window'])} · {esc(m['trials']):} trials · generated {esc(m['today'])}</div>
</div>{('<div class="logo">' + LOGO_SVG + '</div>') if LOGO_SVG else ''}</header>
{panel_divs}
<footer>Monte-Carlo forecast from the team’s actual weekly throughput — the Kanban
replacement for velocity/sprint commitment. Q1/Q2 read at 50/85/95% confidence; quote the
85% line to stakeholders and keep 50% internal. Q3 SLE basis: {esc(m['basis'])}. Valid only
while team &amp; flow match the last {esc(m['sample_weeks'])} weeks — re-run after a reorg or
holiday. Generated by forecast-html.py from a forecast.py JSON dump.</footer>
</main></div></body></html>
""")
    print(f"Wrote {OUT} ({len(panels)} tabs: {', '.join(n for n, _ in panels)})")


if __name__ == "__main__":
    main()
