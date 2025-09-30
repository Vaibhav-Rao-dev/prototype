<template>
  <div>
    <header><h1>Multi-agent Security — Investigation Queue</h1></header>
    <main style="display:flex;gap:16px">
      <section style="flex:1">
        <h2>Playbook Runs</h2>
        <button @click="loadRuns">Refresh</button>
        <ul>
          <li v-for="r in runs" :key="r.id">
            <strong>{{r.playbookId}}</strong> — {{r.status}} <small>({{r.createdAt}})</small>
            <button @click="selectRun(r.id)">Details</button>
          </li>
        </ul>
      </section>
      <section style="flex:2;border-left:1px solid #eee;padding-left:12px">
        <h2>Run Details</h2>
        <div v-if="selected">
          <h3>Steps</h3>
          <ol>
            <li v-for="s in steps" :key="s.index">
              [{{s.index}}] {{s.name}} — {{s.status}} <span v-if="s.requiresApproval">(requires approval: {{s.approverRole}})</span>
              <button v-if="s.requiresApproval && s.status=='pending'" @click="approve(s.index)">Approve</button>
            </li>
          </ol>
          <h3>Map (placeholder)</h3>
          <div style="height:200px;background:#f7f7f7;display:flex;align-items:center;justify-content:center">Map placeholder</div>
          <h3>Timeline (placeholder)</h3>
          <div style="height:120px;background:#fafafa;display:flex;align-items:center;justify-content:center">Timeline placeholder</div>
        </div>
        <div v-else>
          Pick a run to view details
        </div>
      </section>
    </main>
  </div>
</template>

<script>
export default {
  data(){return { runs:[], selected:null, steps:[] }},
  methods:{
    async loadRuns(){ let r = await fetch('/playbook_runs'); this.runs = await r.json(); },
    async selectRun(id){ this.selected = id; let r = await fetch(`/playbook_runs/${id}`); let j = await r.json(); this.steps = j.steps; },
    async approve(idx){ await fetch(`/playbook_runs/${this.selected}/steps/${idx}/approve?role=manager`,{ method:'POST' }); await this.selectRun(this.selected); }
  },
  mounted(){ this.loadRuns(); }
}
</script>

<style>
body{font-family:Arial,Helvetica,sans-serif}
</style>
