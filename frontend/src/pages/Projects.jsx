import { useEffect, useState } from 'react';
import { FolderKanban, Plus, Bug, CheckCircle2, AlertTriangle, Users, UserPlus, X } from 'lucide-react';
import api from '../services/api';
import Modal from '../components/Modal';
import { useAuth } from '../context/AuthContext';

const blankProject = { name: '', description: '', managerId: '' };

export default function Projects() {
  const { user } = useAuth();
  const [projects, setProjects] = useState([]);
  const [bugs, setBugs] = useState([]);
  const [managers, setManagers] = useState([]);
  const [candidates, setCandidates] = useState([]);
  const [modal, setModal] = useState(false);
  const [teamProject, setTeamProject] = useState(null);
  const [memberId, setMemberId] = useState('');
  const [form, setForm] = useState(blankProject);
  const [error, setError] = useState('');

  const load = async () => {
    try {
      const requests = [api.get('/projects'), api.get('/bugs')];
      if (user.role === 'ADMIN') requests.push(api.get('/users/managers'));
      const responses = await Promise.all(requests);
      setProjects(responses[0].data);
      setBugs(responses[1].data);
      if (responses.length > 2) {
        setManagers(responses[2].data);
      } else setManagers([]);
      setError('');
    } catch {
      setError('Projects could not be loaded. Check your connection and access, then retry.');
    }
  };

  useEffect(() => { load(); }, []);

  const create = async (event) => {
    event.preventDefault();
    try {
      await api.post('/projects', { ...form, managerId: form.managerId ? Number(form.managerId) : null });
      setModal(false);
      setForm(blankProject);
      await load();
    } catch (e) {
      setError(e.response?.data?.error || 'Project could not be created.');
    }
  };

  const addMember = async (event) => {
    event.preventDefault();
    if (!teamProject || !memberId) return;
    try {
      await api.post(`/projects/${teamProject.id}/members/${memberId}`);
      setMemberId('');
      await load();
      const updated = await api.get(`/projects/${teamProject.id}`);
      setTeamProject(updated.data);
    } catch (e) {
      setError(e.response?.data?.error || 'Could not add this project member.');
    }
  };

  const removeMember = async (id) => {
    if (!teamProject) return;
    try {
      await api.delete(`/projects/${teamProject.id}/members/${id}`);
      const updated = await api.get(`/projects/${teamProject.id}`);
      setTeamProject(updated.data);
      await load();
    } catch (e) {
      setError(e.response?.data?.error || 'Could not remove this project member.');
    }
  };

  const openTeam = async (project) => {
    setError('');
    try {
      const [projectResponse, candidateResponse] = await Promise.all([
        api.get(`/projects/${project.id}`),
        api.get('/users/project-candidates', { params: { projectId: project.id } })
      ]);
      setTeamProject(projectResponse.data);
      setCandidates(candidateResponse.data);
    } catch (e) {
      setError(e.response?.data?.error || 'Project membership could not be loaded.');
    }
  };

  return <>
    <div className="pageHead">
      <div><div className="eyebrow">WORKSPACE</div><h1>Projects</h1><p>Organize products, modules, teams and issue health.</p></div>
      {['ADMIN', 'MANAGER'].includes(user.role) && <button className="primary" onClick={() => setModal(true)}><Plus size={17}/> New project</button>}
    </div>
    {error && <div className="inlineError" role="alert">{error} <button className="secondary mini" onClick={load}>Retry</button></div>}
    <div className="projectGrid">{projects.map(project => {
      const projectBugs = bugs.filter(bug => bug.project?.id === project.id);
      const open = projectBugs.filter(bug => !['CLOSED', 'RESOLVED'].includes(bug.status)).length;
      const critical = projectBugs.filter(bug => bug.priority === 'CRITICAL').length;
      const canManage = user.role === 'ADMIN' || project.manager?.id === user.id;
      return <div className="projectCard" key={project.id}>
        <div className="projectTop"><div className="projectIcon"><FolderKanban/></div><span className={project.active ? 'live' : 'muted'}>{project.active ? 'ACTIVE' : 'INACTIVE'}</span></div>
        <h3>{project.name}</h3><p>{project.description || 'No description provided.'}</p>
        <div className="projectMetrics"><span><Bug size={14}/> {projectBugs.length} bugs</span><span><CheckCircle2 size={14}/> {projectBugs.filter(bug => ['RESOLVED', 'CLOSED'].includes(bug.status)).length} done</span><span className={critical ? 'dangerText' : ''}><AlertTriangle size={14}/> {critical} critical</span></div>
        <div className="projectFoot"><span>{project.manager?.name || 'No manager'}</span><span>{open} active</span></div>
        <div className="projectFoot"><span><Users size={14}/> {project.members?.length || 0} members</span>{canManage && <button className="secondary mini" onClick={() => openTeam(project)}><UserPlus size={14}/> Manage team</button>}</div>
      </div>;
    })}</div>
    {!projects.length && !error && <div className="empty">No projects are available to your account yet.</div>}

    {modal && <Modal title="Create project" onClose={() => setModal(false)}><form className="formGrid" onSubmit={create}>
      <label className="span2">Project name<input required maxLength="255" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })}/></label>
      <label className="span2">Description<textarea rows="4" maxLength="1000" value={form.description} onChange={e => setForm({ ...form, description: e.target.value })}/></label>
      {user.role === 'ADMIN' && <label className="span2">Project manager<select value={form.managerId} onChange={e => setForm({ ...form, managerId: e.target.value })}><option value="">No manager</option>{managers.map(manager => <option value={manager.id} key={manager.id}>{manager.name}</option>)}</select></label>}
      <div className="modalActions"><button type="button" className="secondary" onClick={() => setModal(false)}>Cancel</button><button className="primary">Create project</button></div>
    </form></Modal>}

    {teamProject && <Modal title={`Manage team · ${teamProject.name}`} onClose={() => setTeamProject(null)}>
      <form className="teamForm" onSubmit={addMember}>
        <label>Add an active user<select required value={memberId} onChange={e => setMemberId(e.target.value)}><option value="">Choose a person</option>{candidates.filter(candidate => !teamProject.members?.some(member => member.id === candidate.id)).map(candidate => <option key={candidate.id} value={candidate.id}>{candidate.name} · {candidate.role}</option>)}</select></label>
        <button className="primary" disabled={!memberId}><UserPlus size={16}/> Add</button>
      </form>
      <div className="teamMembers">
        {(teamProject.members || []).map(member => <div className="teamMember" key={member.id}><span><b>{member.name}</b><small>{member.email} · {member.role}</small></span><button type="button" className="iconBtn" aria-label={`Remove ${member.name}`} onClick={() => removeMember(member.id)}><X size={16}/></button></div>)}
        {!teamProject.members?.length && <div className="empty">No project members have been added.</div>}
      </div>
    </Modal>}
  </>;
}
