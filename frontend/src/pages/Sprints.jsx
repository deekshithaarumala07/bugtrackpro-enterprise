import { useEffect, useState } from 'react';
import { CalendarDays, Plus, ToggleLeft, RefreshCw } from 'lucide-react';
import api from '../services/api';
import Modal from '../components/Modal';
import { useAuth } from '../context/AuthContext';

const today = () => new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10);
const initialForm = () => ({ name: 'Sprint 1', projectId: '', startDate: today(), endDate: new Date(Date.now() + 14 * 86400000 - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10) });

export default function Sprints() {
  const { user } = useAuth();
  const [sprints, setSprints] = useState([]);
  const [projects, setProjects] = useState([]);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState(initialForm);
  const [error, setError] = useState('');
  const load = async () => {
    try {
      const [sprintResponse, projectResponse] = await Promise.all([api.get('/features/sprints'), api.get('/projects')]);
      setSprints(sprintResponse.data); setProjects(projectResponse.data); setError('');
    } catch { setError('Sprints could not be loaded. Check your access or connection and retry.'); }
  };
  useEffect(() => { load(); }, []);
  const manageableProjects = projects.filter(project => user.role === 'ADMIN' || project.manager?.id === user.id);
  const canManage = projectId => user.role === 'ADMIN' || manageableProjects.some(project => String(project.id) === String(projectId));
  const create = async event => {
    event.preventDefault(); setError('');
    if (!form.projectId || form.endDate < form.startDate) { setError('Choose a project and an end date on or after the start date.'); return; }
    try {
      await api.post('/features/sprints', { ...form, projectId: Number(form.projectId) });
      setOpen(false); setForm(initialForm()); await load();
    } catch (e) { setError(e.response?.data?.error || 'Sprint could not be created.'); }
  };
  const toggle = async sprint => {
    setError('');
    try { await api.patch(`/features/sprints/${sprint.id}/toggle`); await load(); }
    catch (e) { setError(e.response?.data?.error || 'Sprint status could not be changed.'); }
  };

  return <>
    <div className="pageHead"><div><div className="eyebrow">DELIVERY PLANNING</div><h1>Sprints</h1><p>Plan work, group issues and monitor delivery windows.</p></div><div className="headActions"><button className="secondary" onClick={load}><RefreshCw size={16}/> Refresh</button>{['ADMIN', 'MANAGER'].includes(user.role) && manageableProjects.length > 0 && <button className="primary" onClick={() => setOpen(true)}><Plus size={16}/> New sprint</button>}</div></div>
    {error && <div className="inlineError" role="alert">{error}</div>}
    <div className="sprintGrid">{sprints.map(sprint => <div className="sprintCard" key={sprint.id}>
      <div className="sprintIcon"><CalendarDays/></div><div><h3>{sprint.name}</h3><span>{sprint.projectName}</span></div><span className={sprint.active ? 'live' : 'muted'}>{sprint.active ? 'ACTIVE' : 'PAUSED'}</span>
      <div className="sprintDates">{sprint.startDate} → {sprint.endDate}</div>
      {canManage(sprint.projectId) && <button className="secondary mini" onClick={() => toggle(sprint)}><ToggleLeft size={15}/>{sprint.active ? 'Complete sprint' : 'Reactivate sprint'}</button>}
    </div>)}</div>
    {!sprints.length && !error && <div className="empty panel">No sprints are available for your projects yet.</div>}
    {open && <Modal title="Create sprint" onClose={() => setOpen(false)}><form className="formGrid" onSubmit={create}>
      <label className="span2">Sprint name<input required maxLength="255" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })}/></label>
      <label className="span2">Project<select required value={form.projectId} onChange={e => setForm({ ...form, projectId: e.target.value })}><option value="">Select project</option>{manageableProjects.map(project => <option key={project.id} value={project.id}>{project.name}</option>)}</select></label>
      <label>Start<input required type="date" value={form.startDate} onChange={e => setForm({ ...form, startDate: e.target.value })}/></label>
      <label>End<input required type="date" min={form.startDate} value={form.endDate} onChange={e => setForm({ ...form, endDate: e.target.value })}/></label>
      <div className="modalActions"><button type="button" className="secondary" onClick={() => setOpen(false)}>Cancel</button><button className="primary">Create sprint</button></div>
    </form></Modal>}
  </>;
}
