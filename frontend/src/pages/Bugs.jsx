import { useEffect, useMemo, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Plus, Search, SlidersHorizontal, BrainCircuit, Copy, Download, RefreshCw } from 'lucide-react';
import api from '../services/api';
import Modal from '../components/Modal';
import StatusBadge from '../components/StatusBadge';
import { useAuth } from '../context/AuthContext';

const empty = { title: '', description: '', projectId: '', priority: 'MEDIUM', severity: 'MAJOR', module: '', category: 'Functional', stepsToReproduce: '', environment: '', assigneeId: '', sprintId: '', dueDate: '', estimatedHours: '' };

export default function Bugs() {
  const { user } = useAuth();
  const [bugs, setBugs] = useState([]);
  const [projects, setProjects] = useState([]);
  const [sprints, setSprints] = useState([]);
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState('ALL');
  const [priority, setPriority] = useState('ALL');
  const [severity, setSeverity] = useState('ALL');
  const [projectFilter, setProjectFilter] = useState('ALL');
  const [modal, setModal] = useState(false);
  const [suggestion, setSuggestion] = useState(null);
  const [duplicates, setDuplicates] = useState([]);
  const [form, setForm] = useState({ ...empty });
  const [searchParams] = useSearchParams();
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      const [bugResponse, projectResponse, sprintResponse] = await Promise.all([
        api.get('/bugs'), api.get('/projects'), api.get('/features/sprints')
      ]);
      setBugs(bugResponse.data); setProjects(projectResponse.data); setSprints(sprintResponse.data); setError('');
    } catch (e) { setError(e.response?.data?.error || 'Issues could not be loaded. Check your access or connection and retry.'); }
    finally { setLoading(false); }
  };
  useEffect(() => { load(); if (searchParams.get('create')) setModal(true); }, []);

  const visibleBugs = useMemo(() => bugs.filter(bug =>
    (status === 'ALL' || bug.status === status)
    && (priority === 'ALL' || bug.priority === priority)
    && (severity === 'ALL' || bug.severity === severity)
    && (projectFilter === 'ALL' || String(bug.project?.id) === projectFilter)
    && (`${bug.title} ${bug.module || ''} ${bug.project?.name || ''} ${bug.category || ''}`).toLowerCase().includes(query.toLowerCase())),
  [bugs, status, priority, severity, projectFilter, query]);

  const project = projects.find(item => String(item.id) === String(form.projectId));
  const canAssign = user.role === 'ADMIN' || (user.role === 'MANAGER' && project?.manager?.id === user.id);
  const developers = (project?.members || []).filter(member => member.active && member.role === 'DEVELOPER');
  const projectSprints = sprints.filter(sprint => sprint.projectId === project?.id);

  const create = async event => {
    event.preventDefault(); setError(''); setLoading(true);
    const payload = { ...form, projectId: Number(form.projectId), assigneeId: form.assigneeId ? Number(form.assigneeId) : null,
      sprintId: form.sprintId ? Number(form.sprintId) : null, estimatedHours: form.estimatedHours ? Number(form.estimatedHours) : null,
      dueDate: form.dueDate ? new Date(form.dueDate).toISOString() : null };
    try { await api.post('/bugs', payload); setModal(false); setForm({ ...empty }); setDuplicates([]); setSuggestion(null); await load(); }
    catch (e) { setError(e.response?.data?.error || 'Issue could not be created.'); }
    finally { setLoading(false); }
  };
  const aiSuggest = async () => {
    if (!form.title.trim()) { setError('Enter an issue title before asking for a suggestion.'); return; }
    setError('');
    try {
      const response = await api.get('/features/ai/suggest', { params: { title: form.title, description: form.description } });
      setSuggestion(response.data);
      setForm(current => ({ ...current, module: current.module || response.data.module, category: response.data.category,
        priority: response.data.priority, severity: response.data.severity,
        stepsToReproduce: current.stepsToReproduce || response.data.suggestedSteps.join('\n') }));
    } catch (e) { setError(e.response?.data?.error || 'Suggestion could not be generated.'); }
  };
  const checkDuplicates = async () => {
    if (!form.title.trim()) { setError('Enter an issue title before checking for duplicates.'); return; }
    setError('');
    try { const response = await api.get('/features/ai/duplicates', { params: { title: form.title, description: form.description } }); setDuplicates(response.data); }
    catch (e) { setError(e.response?.data?.error || 'Duplicate search could not run.'); }
  };
  const exportCsv = async () => {
    setError('');
    try {
      const response = await api.get('/features/reports/bugs.csv', { responseType: 'blob' });
      const url = URL.createObjectURL(response.data); const anchor = document.createElement('a');
      anchor.href = url; anchor.download = 'bug-report.csv'; anchor.click(); window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (e) { setError(e.response?.data?.error || 'CSV export could not be generated.'); }
  };

  return <>
    <div className="pageHead"><div><div className="eyebrow">ISSUE TRACKER</div><h1>All Bugs</h1><p>Search, triage, assign, prioritize and monitor SLA health.</p></div><div className="headActions"><button className="secondary" onClick={exportCsv}><Download size={16}/> CSV</button><button className="primary" onClick={() => { setError(''); setModal(true); }}><Plus size={17}/> Report bug</button></div></div>
    {error && <div className="inlineError" role="alert">{error}</div>}
    <div className="toolbar wrap"><div className="search"><Search size={17}/><input type="search" placeholder="Search title, module, project, category…" value={query} onChange={event => setQuery(event.target.value)}/></div>
      <div className="filter"><SlidersHorizontal size={16}/><select value={status} onChange={event => setStatus(event.target.value)}><option value="ALL">All statuses</option>{['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REOPENED'].map(value => <option key={value}>{value}</option>)}</select></div>
      <select value={priority} onChange={event => setPriority(event.target.value)}><option value="ALL">All priorities</option>{['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'].map(value => <option key={value}>{value}</option>)}</select>
      <select value={severity} onChange={event => setSeverity(event.target.value)}><option value="ALL">All severity</option>{['MINOR', 'MAJOR', 'BLOCKER', 'CRITICAL'].map(value => <option key={value}>{value}</option>)}</select>
      <select value={projectFilter} onChange={event => setProjectFilter(event.target.value)}><option value="ALL">All projects</option>{projects.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select>
      <button className="secondary mini" onClick={load} aria-label="Refresh issues"><RefreshCw size={15}/></button>
    </div>
    <section className="panel"><div className="tableWrap"><table className="bugsTable"><thead><tr><th>ID / Issue</th><th>Project</th><th>Severity</th><th>Priority</th><th>Assignee</th><th>SLA</th><th>Status</th></tr></thead><tbody>
      {visibleBugs.map(bug => <tr key={bug.id}><td><Link className="issueLink" to={`/bugs/${bug.id}`}>#{bug.id} {bug.title}</Link><small>{bug.category || 'Functional'} · {bug.module || 'General'}</small></td><td>{bug.project?.name}</td><td><span className={`severity s-${bug.severity.toLowerCase()}`}>{bug.severity}</span></td><td><span className={`priority p-${bug.priority.toLowerCase()}`}>{bug.priority}</span></td><td>{bug.assignee?.name || <span className="muted">Unassigned</span>}</td><td>{bug.slaDueAt && new Date(bug.slaDueAt) < new Date() && bug.status !== 'CLOSED' ? <span className="overdue">BREACHED</span> : <span className="muted">{bug.slaDueAt ? new Date(bug.slaDueAt).toLocaleDateString() : '—'}</span>}</td><td><StatusBadge value={bug.status}/></td></tr>)}
    </tbody></table>{!visibleBugs.length && !loading && <div className="empty">No matching issues found.</div>}{loading && <div className="loading">Refreshing issues…</div>}</div></section>

    {modal && <Modal title="Report a new bug" onClose={() => setModal(false)}><form className="formGrid" onSubmit={create}>
      <label className="span2">Title<input required maxLength="255" value={form.title} onChange={event => setForm({ ...form, title: event.target.value })}/></label>
      <div className="aiActions span2"><button type="button" className="secondary" onClick={aiSuggest}><BrainCircuit size={16}/> Suggest triage</button><button type="button" className="secondary" onClick={checkDuplicates}><Copy size={16}/> Check duplicates</button>{suggestion && <span className="aiHint">Suggested {suggestion.module} · {suggestion.severity} · {suggestion.priority}</span>}</div>
      {duplicates.length > 0 && <div className="duplicateBox span2"><b>Possible duplicates</b>{duplicates.map(item => <Link key={item.bug.id} to={`/bugs/${item.bug.id}`} onClick={() => setModal(false)}>#{item.bug.id} {item.bug.title} — {item.similarity}% similar</Link>)}</div>}
      <label>Project<select required value={form.projectId} onChange={event => setForm({ ...form, projectId: event.target.value, assigneeId: '', sprintId: '' })}><option value="">Select project</option>{projects.map(item => <option value={item.id} key={item.id}>{item.name}</option>)}</select></label>
      <label>Module<input maxLength="120" value={form.module} onChange={event => setForm({ ...form, module: event.target.value })}/></label>
      <label>Category<select value={form.category} onChange={event => setForm({ ...form, category: event.target.value })}>{['Functional', 'UI', 'Performance', 'Security', 'Data', 'Infrastructure'].map(value => <option key={value}>{value}</option>)}</select></label>
      <label>Priority<select value={form.priority} onChange={event => setForm({ ...form, priority: event.target.value })}>{['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'].map(value => <option key={value}>{value}</option>)}</select></label>
      <label>Severity<select value={form.severity} onChange={event => setForm({ ...form, severity: event.target.value })}>{['MINOR', 'MAJOR', 'BLOCKER', 'CRITICAL'].map(value => <option key={value}>{value}</option>)}</select></label>
      {canAssign && <label>Assignee<select value={form.assigneeId} onChange={event => setForm({ ...form, assigneeId: event.target.value })}><option value="">Unassigned</option>{developers.map(member => <option key={member.id} value={member.id}>{member.name}</option>)}</select></label>}
      <label>Sprint<select value={form.sprintId} onChange={event => setForm({ ...form, sprintId: event.target.value })}><option value="">No sprint</option>{projectSprints.map(sprint => <option key={sprint.id} value={sprint.id}>{sprint.name}</option>)}</select></label>
      <label>Due date<input type="datetime-local" value={form.dueDate} onChange={event => setForm({ ...form, dueDate: event.target.value })}/></label>
      <label>Estimated hours<input type="number" min="0" step="0.5" value={form.estimatedHours} onChange={event => setForm({ ...form, estimatedHours: event.target.value })}/></label>
      <label>Environment<input maxLength="1000" value={form.environment} onChange={event => setForm({ ...form, environment: event.target.value })} placeholder="Browser, OS, app version…"/></label>
      <label className="span2">Description<textarea required maxLength="10000" rows="4" value={form.description} onChange={event => setForm({ ...form, description: event.target.value })}/></label>
      <label className="span2">Steps to reproduce<textarea maxLength="2000" rows="4" value={form.stepsToReproduce} onChange={event => setForm({ ...form, stepsToReproduce: event.target.value })}/></label>
      {error && <div className="inlineError span2" role="alert">{error}</div>}
      <div className="modalActions"><button type="button" className="secondary" onClick={() => setModal(false)}>Cancel</button><button className="primary" disabled={loading || !form.projectId}>Create issue</button></div>
    </form></Modal>}
  </>;
}
