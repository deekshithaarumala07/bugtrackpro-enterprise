import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, Paperclip, Send, Download, UserRound, History, Clock3, BrainCircuit, RefreshCw } from 'lucide-react';
import api from '../services/api';
import StatusBadge from '../components/StatusBadge';
import { useAuth } from '../context/AuthContext';

const transitions = { OPEN: ['IN_PROGRESS', 'REOPENED'], IN_PROGRESS: ['OPEN', 'RESOLVED'], RESOLVED: ['IN_PROGRESS', 'CLOSED', 'REOPENED'], CLOSED: ['REOPENED'], REOPENED: ['IN_PROGRESS', 'RESOLVED'] };

export default function BugDetails() {
  const { id } = useParams();
  const { user } = useAuth();
  const [bug, setBug] = useState(null);
  const [comments, setComments] = useState([]);
  const [files, setFiles] = useState([]);
  const [history, setHistory] = useState([]);
  const [sprints, setSprints] = useState([]);
  const [comment, setComment] = useState('');
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [suggestion, setSuggestion] = useState(null);

  const load = async () => {
    setLoading(true);
    try {
      const [bugResponse, commentResponse, fileResponse, historyResponse, sprintResponse] = await Promise.all([
        api.get(`/bugs/${id}`), api.get(`/bugs/${id}/comments`), api.get(`/bugs/${id}/attachments`),
        api.get(`/bugs/${id}/history`), api.get('/features/sprints')
      ]);
      setBug(bugResponse.data); setComments(commentResponse.data); setFiles(fileResponse.data);
      setHistory(historyResponse.data); setSprints(sprintResponse.data); setError('');
    } catch (e) {
      setError(e.response?.data?.error || 'Issue details could not be loaded.');
    } finally { setLoading(false); }
  };
  useEffect(() => { load(); }, [id]);

  if (loading && !bug) return <div className="loading">Loading issue…</div>;
  if (!bug) return <><div className="inlineError" role="alert">{error || 'This issue is unavailable to your account.'}</div><button className="secondary" onClick={load}><RefreshCw size={16}/> Retry</button></>;

  const canManage = user.role === 'ADMIN' || (user.role === 'MANAGER' && bug.project?.manager?.id === user.id);
  const canChangeStatus = canManage || (user.role === 'DEVELOPER' && bug.assignee?.id === user.id) || (user.role === 'TESTER' && bug.reporter?.id === user.id);
  const nextStatuses = (transitions[bug.status] || []).filter(status => canManage
    || (user.role === 'DEVELOPER' && ['IN_PROGRESS', 'RESOLVED'].includes(status))
    || (user.role === 'TESTER' && status === 'REOPENED' && bug.reporter?.id === user.id));
  const developers = (bug.project?.members || []).filter(member => member.active && member.role === 'DEVELOPER');
  const visibleDevelopers = bug.assignee && !developers.some(member => member.id === bug.assignee.id) ? [...developers, bug.assignee] : developers;
  const projectSprints = sprints.filter(sprint => sprint.projectId === bug.project?.id);
  const breached = bug.slaDueAt && new Date(bug.slaDueAt) < new Date() && bug.status !== 'CLOSED';

  const act = async (request, successMessage) => {
    setBusy(true); setError('');
    try { await request(); await load(); if (successMessage) setError(''); }
    catch (e) { setError(e.response?.data?.error || 'The change was rejected. The issue was not updated.'); }
    finally { setBusy(false); }
  };
  const changeStatus = event => act(() => api.patch(`/bugs/${id}/status`, { status: event.target.value }));
  const changeAssignee = event => act(() => api.patch(`/bugs/${id}/assign`, { assigneeId: event.target.value ? Number(event.target.value) : null }));
  const changeSprint = event => act(() => api.patch(`/features/bugs/${id}/sprint`, { sprintId: event.target.value ? Number(event.target.value) : null }));
  const addComment = async event => {
    event.preventDefault(); if (!comment.trim()) return;
    setBusy(true); setError('');
    try { await api.post(`/bugs/${id}/comments`, { content: comment.trim() }); setComment(''); await load(); }
    catch (e) { setError(e.response?.data?.error || 'Comment could not be posted.'); }
    finally { setBusy(false); }
  };
  const upload = async event => {
    const file = event.target.files?.[0]; if (!file) return;
    const form = new FormData(); form.append('file', file);
    setBusy(true); setError('');
    try { await api.post(`/bugs/${id}/attachments`, form); event.target.value = ''; await load(); }
    catch (e) { setError(e.response?.data?.error || 'Attachment could not be uploaded.'); event.target.value = ''; }
    finally { setBusy(false); }
  };
  const download = async attachment => {
    setError('');
    try {
      const response = await api.get(`/bugs/attachments/${attachment.id}/download`, { responseType: 'blob' });
      const url = URL.createObjectURL(response.data); const anchor = document.createElement('a');
      anchor.href = url; anchor.download = attachment.originalName; anchor.click(); URL.revokeObjectURL(url);
    } catch (e) { setError(e.response?.data?.error || 'Attachment could not be downloaded.'); }
  };
  const analyze = async () => {
    setError('');
    try { const result = await api.get('/features/ai/suggest', { params: { title: bug.title, description: bug.description } }); setSuggestion(result.data); }
    catch (e) { setError(e.response?.data?.error || 'Issue triage could not run.'); }
  };

  return <>
    <Link className="back" to="/bugs"><ArrowLeft size={16}/> Back to bugs</Link>
    {error && <div className="inlineError" role="alert">{error}</div>}
    <div className="detailHead"><div><div className="eyebrow">BUG #{bug.id}</div><h1>{bug.title}</h1><div className="metaLine"><StatusBadge value={bug.status}/><span>Reported by {bug.reporter?.name}</span><span>Updated {new Date(bug.updatedAt).toLocaleString()}</span>{breached && <span className="overdue">SLA BREACHED</span>}</div></div>
      {canChangeStatus && nextStatuses.length > 0 && <select className="statusSelect" value={bug.status} disabled={busy} onChange={changeStatus} aria-label="Issue status">{[bug.status, ...nextStatuses].map(status => <option key={status} value={status}>{status.replaceAll('_', ' ')}</option>)}</select>}
    </div>
    <div className="detailGrid"><main>
      <section className="panel detailPanel"><div className="panelHead"><div><h2>Description</h2><p>Issue report and reproduction context</p></div><span className={`priority p-${bug.priority.toLowerCase()}`}>{bug.priority} priority</span></div>
        <p className="description">{bug.description}</p><div className="infoGrid"><div><span>Severity</span><b>{bug.severity}</b></div><div><span>Category</span><b>{bug.category || 'Functional'}</b></div><div><span>Module</span><b>{bug.module || 'General'}</b></div><div><span>Environment</span><b>{bug.environment || 'Not specified'}</b></div><div><span>Project</span><b>{bug.project?.name}</b></div><div><span>SLA</span><b className={breached ? 'dangerText' : ''}>{bug.slaDueAt ? new Date(bug.slaDueAt).toLocaleString() : 'Auto'}</b></div></div>
        {bug.stepsToReproduce && <div className="subsection"><h3>Steps to reproduce</h3><pre>{bug.stepsToReproduce}</pre></div>}
      </section>
      <section className="panel"><div className="panelHead"><div><h2>Discussion</h2><p>Comments are visible to people with access to this issue.</p></div></div>
        <div className="comments">{comments.map(item => <div className="comment" key={item.id}><div className="avatar small">{item.author?.name?.slice(0, 1)}</div><div><div><b>{item.author?.name}</b><span className="muted"> · {new Date(item.createdAt).toLocaleString()}</span></div><p>{item.content}</p></div></div>)}{!comments.length && <div className="empty">No comments yet. Start the discussion.</div>}</div>
        <form className="commentForm" onSubmit={addComment}><input maxLength="2000" value={comment} onChange={event => setComment(event.target.value)} placeholder="Write a comment…" aria-label="Comment"/><button className="primary" disabled={busy || !comment.trim()} aria-label="Post comment"><Send size={16}/></button></form>
      </section>
      <section className="panel"><div className="panelHead"><div><h2><History size={18}/> Activity history</h2><p>Recorded issue changes</p></div></div><div className="timeline">{history.map(item => <div className="timelineItem" key={item.id}><span className="timelineDot"/><div><b>{item.action.replaceAll('_', ' ')}</b><p>{item.details}</p><small>{item.actor?.name || 'System'} · {new Date(item.createdAt).toLocaleString()}</small></div></div>)}</div>{!history.length && <div className="empty">No activity recorded yet.</div>}</section>
    </main><aside>
      <section className="panel sidePanel"><h3><UserRound size={17}/> Assignment</h3>
        <label>Current assignee<select value={bug.assignee?.id || ''} onChange={changeAssignee} disabled={!canManage || busy}><option value="">Unassigned</option>{visibleDevelopers.map(member => <option key={member.id} value={member.id}>{member.name} · {member.role}</option>)}</select></label>
        <label>Sprint<select value={bug.sprint?.id || ''} onChange={changeSprint} disabled={!canManage || busy}><option value="">Backlog</option>{projectSprints.map(sprint => <option key={sprint.id} value={sprint.id}>{sprint.name}</option>)}</select></label>
        <div className="reporter"><span>Reporter</span><b>{bug.reporter?.name}</b><small>{bug.reporter?.email}</small></div>
      </section>
      <section className="panel sidePanel"><div className="sideTitle"><h3><Paperclip size={17}/> Attachments</h3><label className="uploadBtn">+ Add<input type="file" accept=".png,.jpg,.jpeg,.gif,.pdf,.txt,.csv,.log" disabled={busy} onChange={upload}/></label></div>
        {files.map(file => <div className="fileRow" key={file.id}><div><b>{file.originalName}</b><small>{Math.round(file.size / 1024)} KB</small></div><button className="iconBtn" type="button" onClick={() => download(file)} aria-label={`Download ${file.originalName}`}><Download size={16}/></button></div>)}{!files.length && <div className="empty smallEmpty">No attachments.</div>}
      </section>
      <section className="panel sidePanel"><h3><BrainCircuit size={17}/> Local triage suggestion</h3><p className="muted">Uses deterministic text rules; it does not call an AI service.</p><button className="secondary" onClick={analyze}><BrainCircuit size={15}/> Analyze issue</button>{suggestion && <div className="aiHint">{suggestion.module} · {suggestion.severity} · {suggestion.priority}</div>}</section>
      <section className="panel sidePanel"><h3><Clock3 size={17}/> SLA</h3><div className="slaBox"><b className={breached ? 'dangerText' : 'successText'}>{breached ? 'BREACHED' : 'ON TRACK'}</b><span>Due {bug.slaDueAt ? new Date(bug.slaDueAt).toLocaleString() : 'automatically calculated'}</span></div></section>
    </aside></div>
  </>;
}
