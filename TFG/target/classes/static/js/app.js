(() => {
  const STORAGE_KEY = "easy4you.auth";

  function getAuth() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return null;
      const parsed = JSON.parse(raw);
      if (!parsed || typeof parsed !== "object") return null;
      return parsed;
    } catch {
      return null;
    }
  }

  function setAuth(auth) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
  }

  function clearAuth() {
    localStorage.removeItem(STORAGE_KEY);
  }

  function getToken() {
    return getAuth()?.token || null;
  }

  function requireAuth() {
    const token = getToken();
    if (!token) {
      window.location.href = "/app/login";
      return null;
    }
    return token;
  }

  function qs(id) {
    return document.getElementById(id);
  }

  function toast(message, variant = "info") {
    const root = qs("toast-root");
    if (!root) return;

    const colors = {
      info: "border-slate-700 bg-slate-950/70 text-slate-100",
      success: "border-emerald-900/50 bg-emerald-950/50 text-emerald-100",
      error: "border-rose-900/50 bg-rose-950/50 text-rose-100",
    };

    const el = document.createElement("div");
    el.className = `pointer-events-auto max-w-[420px] rounded-2xl border px-4 py-3 text-sm shadow-xl backdrop-blur ${colors[variant] || colors.info}`;
    el.textContent = message;

    root.appendChild(el);
    setTimeout(() => {
      el.style.opacity = "0";
      el.style.transition = "opacity 240ms ease";
      setTimeout(() => el.remove(), 260);
    }, 3400);
  }

  function openModal(title, bodyHtml) {
    const root = qs("modal-root");
    if (!root) return () => {};

    root.classList.remove("hidden");
    root.innerHTML = `
      <div class="fixed inset-0 bg-slate-950/60 backdrop-blur-sm"></div>
      <div class="fixed inset-0 flex items-center justify-center p-6">
        <div class="w-full max-w-lg rounded-2xl border border-slate-800 bg-slate-900/60 shadow-2xl">
          <div class="flex items-center justify-between gap-3 border-b border-slate-800 px-5 py-4">
            <h2 class="text-sm font-semibold">${title}</h2>
            <button id="e4y-modal-close" class="rounded-xl border border-slate-800 bg-slate-950/20 px-3 py-1.5 text-xs font-medium text-slate-200 hover:border-brand-500">Cerrar</button>
          </div>
          <div class="px-5 py-4">${bodyHtml}</div>
        </div>
      </div>
    `;

    const close = () => {
      root.innerHTML = "";
      root.classList.add("hidden");
    };

    const closeBtn = qs("e4y-modal-close");
    if (closeBtn) closeBtn.addEventListener("click", close);
    root.querySelector(".fixed.inset-0.bg-slate-950\\/60")?.addEventListener("click", close);

    return close;
  }

  function buildApiHeaders(extra = {}) {
    const token = getToken();
    const headers = { Accept: "application/json", ...extra };
    if (token) headers.Authorization = `Bearer ${token}`;
    return headers;
  }

  async function apiFetch(path, { method = "GET", body, headers = {}, raw = false } = {}) {
    const init = { method, headers: buildApiHeaders(headers) };

    if (body instanceof FormData) {
      init.body = body;
    } else if (body !== undefined && body !== null) {
      init.headers["Content-Type"] = "application/json";
      init.body = JSON.stringify(body);
    }

    const res = await fetch(path, init);
    if (res.status === 401) {
      clearAuth();
      window.location.href = "/app/login";
      return null;
    }

    const contentType = res.headers.get("content-type") || "";
    const isJson = contentType.includes("application/json");
    const payload = raw ? await res.text() : isJson ? await res.json().catch(() => null) : await res.text();

    if (!res.ok) {
      const msg = payload?.message || payload?.error || (typeof payload === "string" ? payload : null);
      const err = new Error(msg || `Error HTTP ${res.status}`);
      err.status = res.status;
      err.payload = payload;
      throw err;
    }

    return payload;
  }

  function fmtEstadoDocumento(estado) {
    const e = (estado || "").toUpperCase();
    if (e === "PROCESADO" || e === "LISTO") return { label: "Procesado", cls: "bg-emerald-950/40 text-emerald-200 border-emerald-900/50" };
    if (e === "PROCESANDO") return { label: "Procesando", cls: "bg-amber-950/40 text-amber-200 border-amber-900/50" };
    if (e === "ERROR") return { label: "Error", cls: "bg-rose-950/40 text-rose-200 border-rose-900/50" };
    return { label: e || "Pendiente", cls: "bg-slate-950/40 text-slate-200 border-slate-800" };
  }

  function truncate(text, max = 180) {
    const t = (text || "").trim();
    if (t.length <= max) return t;
    return t.slice(0, max - 1) + "…";
  }

  function normalizeHex(hex) {
    if (!hex) return null;
    const h = String(hex).trim();
    if (!h) return null;
    return h.startsWith("#") ? h : `#${h}`;
  }

  function setupLogout() {
    const btn = qs("logout-btn");
    if (!btn) return;
    btn.addEventListener("click", () => {
      clearAuth();
      window.location.href = "/app/login";
    });
  }

  async function initLogin() {
    const token = getToken();
    if (token) {
      window.location.href = "/app/notebooks";
      return;
    }

    const loginForm = qs("login-form");
    const registerForm = qs("register-form");

    if (loginForm) {
      loginForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        const email = qs("login-email")?.value?.trim();
        const password = qs("login-password")?.value || "";
        if (!email || !password) return;

        try {
          const res = await apiFetch("/api/auth/login", { method: "POST", body: { email, password } });
          setAuth({ token: res.token, type: res.type, userId: res.userId, email: res.email, roles: res.roles });
          toast("Login correcto", "success");
          window.location.href = "/app/notebooks";
        } catch (err) {
          toast(err.message || "No se pudo iniciar sesión", "error");
        }
      });
    }

    if (registerForm) {
      registerForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        const nombre = qs("reg-nombre")?.value?.trim();
        const apellidos = qs("reg-apellidos")?.value?.trim();
        const email = qs("reg-email")?.value?.trim();
        const password = qs("reg-password")?.value || "";
        if (!nombre || !apellidos || !email || !password) return;

        try {
          const res = await apiFetch("/api/auth/register", {
            method: "POST",
            body: { nombre, apellidos, email, password },
          });
          setAuth({ token: res.token, type: res.type, userId: res.userId, email: res.email, roles: res.roles });
          toast("Cuenta creada", "success");
          window.location.href = "/app/notebooks";
        } catch (err) {
          toast(err.message || "No se pudo registrar", "error");
        }
      });
    }
  }

  async function initNotebookIndex() {
    requireAuth();

    setupLogout();

    const list = qs("notebooks-list");
    const empty = qs("notebooks-empty");
    const sharedList = qs("shared-list");
    const sharedEmpty = qs("shared-empty");
    const openBtn = qs("open-create-notebook");

    async function refresh() {
      if (list) list.innerHTML = "";
      if (sharedList) sharedList.innerHTML = "";
      if (empty) empty.classList.add("hidden");
      if (sharedEmpty) sharedEmpty.classList.add("hidden");

      try {
        const notebooks = await apiFetch("/api/notebooks");
        if (!Array.isArray(notebooks) || notebooks.length === 0) {
          empty?.classList.remove("hidden");
        } else {
          notebooks.forEach((nb) => {
            const a = document.createElement("a");
            a.href = `/app/notebooks/${nb.id}`;
            a.className =
              "group rounded-2xl border border-slate-800 bg-slate-900/30 p-5 hover:border-brand-500 hover:bg-slate-900/40";

            const badge = document.createElement("div");
            const color = normalizeHex(nb.colorHex) || "#6D4CFF";
            badge.className = "h-2 w-10 rounded-full";
            badge.style.background = color;

            const title = document.createElement("h3");
            title.className = "mt-4 text-lg font-semibold tracking-tight";
            title.textContent = nb.nombre || `Notebook ${nb.id}`;

            const desc = document.createElement("p");
            desc.className = "mt-1 text-sm text-slate-400";
            desc.textContent = truncate(nb.descripcion || "Sin descripción", 120);

            const meta = document.createElement("p");
            meta.className = "mt-4 text-xs text-slate-500";
            meta.textContent = "Abrir notebook →";

            a.appendChild(badge);
            a.appendChild(title);
            a.appendChild(desc);
            a.appendChild(meta);
            list?.appendChild(a);
          });
        }
      } catch (err) {
        toast(err.message || "No se pudieron cargar notebooks", "error");
      }

      try {
        const shared = await apiFetch("/api/notebooks/compartidos-conmigo");
        if (!Array.isArray(shared) || shared.length === 0) {
          sharedEmpty?.classList.remove("hidden");
          return;
        }

        shared.forEach((s) => {
          const card = document.createElement("div");
          card.className = "rounded-2xl border border-slate-800 bg-slate-900/20 p-4";

          const title = document.createElement("p");
          title.className = "text-sm font-semibold";
          title.textContent = `Notebook #${s.asignaturaId}`;

          const meta = document.createElement("p");
          meta.className = "mt-1 text-xs text-slate-500";
          meta.textContent = `Rol: ${s.rol || "VIEWER"} · Propietario: ${s.propietarioId}`;

          const hint = document.createElement("p");
          hint.className = "mt-3 text-xs text-slate-400";
          hint.textContent = "Vista completa de notebooks compartidos: pendiente de UX/permiso.";

          card.appendChild(title);
          card.appendChild(meta);
          card.appendChild(hint);
          sharedList?.appendChild(card);
        });
      } catch {
        // optional
      }
    }

    function openCreateNotebookModal() {
      const close = openModal(
        "Nuevo notebook",
        `
        <form id="create-notebook-form" class="space-y-3">
          <div>
            <label class="block text-xs font-medium text-slate-400">Nombre</label>
            <input id="nb-nombre" required class="mt-1.5 w-full rounded-xl border border-slate-800 bg-slate-950/20 px-3 py-2 text-sm text-slate-200 outline-none focus:border-brand-500" placeholder="Ej: Redes 1" />
          </div>
          <div>
            <label class="block text-xs font-medium text-slate-400">Descripción</label>
            <textarea id="nb-descripcion" rows="3" class="mt-1.5 w-full rounded-xl border border-slate-800 bg-slate-950/20 px-3 py-2 text-sm text-slate-200 outline-none focus:border-brand-500" placeholder="Opcional"></textarea>
          </div>
          <div>
            <label class="block text-xs font-medium text-slate-400">Color</label>
            <input id="nb-color" type="color" value="#6D4CFF" class="mt-1.5 h-10 w-full rounded-xl border border-slate-800 bg-slate-950/20 px-2" />
          </div>
          <button type="submit" class="mt-2 w-full rounded-xl bg-brand-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-brand-500">Crear</button>
        </form>
      `
      );

      const form = qs("create-notebook-form");
      if (!form) return;
      form.addEventListener("submit", async (e) => {
        e.preventDefault();
        const nombre = qs("nb-nombre")?.value?.trim();
        const descripcion = qs("nb-descripcion")?.value?.trim();
        const colorHex = qs("nb-color")?.value?.trim();
        if (!nombre) return;
        try {
          const created = await apiFetch("/api/notebooks", {
            method: "POST",
            body: { nombre, descripcion: descripcion || null, colorHex: colorHex || null },
          });
          close();
          toast("Notebook creado", "success");
          window.location.href = `/app/notebooks/${created.id}`;
        } catch (err) {
          toast(err.message || "No se pudo crear el notebook", "error");
        }
      });
    }

    openBtn?.addEventListener("click", openCreateNotebookModal);

    await refresh();
  }

  async function initNotebookDetail() {
    requireAuth();
    setupLogout();

    const notebookId = Number(document.body.dataset.notebookId);
    if (!notebookId) {
      toast("Notebook inválido", "error");
      return;
    }

    const sourcesList = qs("sources-list");
    const sourcesSearch = qs("sources-search");
    const notebookTitle = qs("notebook-title");

    const tabChat = qs("tab-chat");
    const tabDoc = qs("tab-document");
    const chatPanel = qs("chat-panel");
    const documentPanel = qs("document-panel");

    const conversationSelect = qs("conversation-select");
    const newConversationBtn = qs("new-conversation-btn");
    const chatMessages = qs("chat-messages");
    const chatForm = qs("chat-form");
    const chatInput = qs("chat-input");

    const docTitle = qs("doc-title");
    const docStatus = qs("doc-status");
    const docError = qs("doc-error");
    const docChunks = qs("doc-chunks");
    const docArtefacts = qs("doc-artefacts");

    const genResumenBtn = qs("gen-resumen-btn");
    const genFlashcardsBtn = qs("gen-flashcards-btn");
    const genTestBtn = qs("gen-test-btn");

    const resumenesList = qs("resumenes-list");
    const flashcardsCount = qs("flashcards-count");
    const testsCount = qs("tests-count");
    const startFlashcards = qs("start-flashcards");
    const startTest = qs("start-test");
    const notesList = qs("notes-list");
    const progressBox = qs("progress-box");

    let currentDocs = [];
    let currentDocId = null;
    let currentConversationId = null;

    function setActiveTab(which) {
      const isChat = which === "chat";
      if (tabChat) {
        tabChat.dataset.active = isChat ? "true" : "false";
        tabChat.className = isChat
          ? "rounded-lg px-3 py-1.5 text-xs font-semibold text-slate-100 bg-slate-950/40"
          : "rounded-lg px-3 py-1.5 text-xs font-semibold text-slate-400";
      }
      if (tabDoc) {
        tabDoc.className = !isChat
          ? "rounded-lg px-3 py-1.5 text-xs font-semibold text-slate-100 bg-slate-950/40"
          : "rounded-lg px-3 py-1.5 text-xs font-semibold text-slate-400";
      }

      chatPanel?.classList.toggle("hidden", !isChat);
      documentPanel?.classList.toggle("hidden", isChat);
    }

    tabChat?.addEventListener("click", () => setActiveTab("chat"));
    tabDoc?.addEventListener("click", () => setActiveTab("doc"));

    function renderSources() {
      if (!sourcesList) return;
      sourcesList.innerHTML = "";

      const q = (sourcesSearch?.value || "").trim().toLowerCase();
      const docs = q ? currentDocs.filter((d) => (d.nombreOriginal || "").toLowerCase().includes(q)) : currentDocs;

      if (!docs.length) {
        const p = document.createElement("p");
        p.className = "text-sm text-slate-500";
        p.textContent = "No hay documentos.";
        sourcesList.appendChild(p);
        return;
      }

      docs.forEach((d) => {
        const estado = fmtEstadoDocumento(d.estadoProcesado);
        const btn = document.createElement("button");
        btn.className =
          "w-full rounded-xl border border-slate-800 bg-slate-950/20 px-3 py-2 text-left hover:border-brand-500";
        btn.addEventListener("click", () => {
          currentDocId = d.id;
          loadDocumentoDetalle(d.id);
          setActiveTab("doc");
        });

        const row = document.createElement("div");
        row.className = "flex items-start justify-between gap-3";

        const left = document.createElement("div");
        const name = document.createElement("p");
        name.className = "text-sm font-semibold text-slate-100";
        name.textContent = truncate(d.nombreOriginal || `Documento ${d.id}`, 52);
        const meta = document.createElement("p");
        meta.className = "mt-0.5 text-xs text-slate-500";
        meta.textContent = d.temaId ? `Tema #${d.temaId}` : "Sin tema";
        left.appendChild(name);
        left.appendChild(meta);

        const badge = document.createElement("span");
        badge.className = `inline-flex items-center rounded-full border px-2 py-1 text-[10px] font-semibold ${estado.cls}`;
        badge.textContent = estado.label;

        row.appendChild(left);
        row.appendChild(badge);
        btn.appendChild(row);
        sourcesList.appendChild(btn);
      });
    }

    sourcesSearch?.addEventListener("input", () => renderSources());

    function renderOverview(data) {
      notebookTitle.textContent = data?.notebook?.nombre || `Notebook ${notebookId}`;

      currentDocs = Array.isArray(data.documentos) ? data.documentos : [];
      renderSources();

      if (resumenesList) {
        resumenesList.innerHTML = "";
        const items = Array.isArray(data.resumenes) ? data.resumenes.slice(0, 3) : [];
        if (!items.length) {
          const p = document.createElement("p");
          p.className = "text-sm text-slate-500";
          p.textContent = "Aún no hay resúmenes.";
          resumenesList.appendChild(p);
        } else {
          items.forEach((r) => {
            const el = document.createElement("div");
            el.className = "rounded-xl border border-slate-800 bg-slate-950/20 p-3";
            const t = document.createElement("p");
            t.className = "text-sm font-semibold";
            t.textContent = truncate(r.titulo || "Resumen", 80);
            const c = document.createElement("p");
            c.className = "mt-1 text-xs text-slate-400";
            c.textContent = truncate(r.contenido || "", 110);
            el.appendChild(t);
            el.appendChild(c);
            resumenesList.appendChild(el);
          });
        }
      }

      const flashCount = Array.isArray(data.flashcards) ? data.flashcards.length : 0;
      const testCount = Array.isArray(data.preguntas) ? data.preguntas.length : 0;
      flashcardsCount.textContent = `${flashCount} flashcards`;
      testsCount.textContent = `${testCount} preguntas`;

      if (notesList) {
        notesList.innerHTML = "";
        const items = Array.isArray(data.notas) ? data.notas.slice(0, 4) : [];
        if (!items.length) {
          const p = document.createElement("p");
          p.className = "text-sm text-slate-500";
          p.textContent = "Aún no hay notas.";
          notesList.appendChild(p);
        } else {
          items.forEach((n) => {
            const el = document.createElement("div");
            el.className = "rounded-xl border border-slate-800 bg-slate-950/20 p-3";
            const t = document.createElement("p");
            t.className = "text-sm font-semibold";
            t.textContent = truncate(n.titulo || "Nota", 70);
            const c = document.createElement("p");
            c.className = "mt-1 text-xs text-slate-400";
            c.textContent = truncate(n.contenido || "", 110);
            el.appendChild(t);
            el.appendChild(c);
            notesList.appendChild(el);
          });
        }
      }
    }

    async function refreshOverview() {
      const data = await apiFetch(`/api/notebooks/${notebookId}/overview`);
      renderOverview(data);
      return data;
    }

    function renderChatMessage(msg, sources = null) {
      if (!chatMessages) return;
      const isUser = (msg.rol || "").toUpperCase() === "USER";

      const wrap = document.createElement("div");
      wrap.className = `flex ${isUser ? "justify-end" : "justify-start"}`;

      const bubble = document.createElement("div");
      bubble.className = isUser
        ? "max-w-[85%] rounded-2xl bg-brand-600/20 px-4 py-3 text-sm text-slate-100"
        : "max-w-[85%] rounded-2xl border border-slate-800 bg-slate-950/20 px-4 py-3 text-sm text-slate-100";

      const content = document.createElement("div");
      content.className = "whitespace-pre-wrap";
      content.textContent = msg.contenido || "";
      bubble.appendChild(content);

      const fuentes = sources || msg.fuentes;
      if (!isUser && Array.isArray(fuentes) && fuentes.length) {
        const srcBox = document.createElement("div");
        srcBox.className = "mt-3 border-t border-slate-800 pt-2 text-xs text-slate-300";
        const title = document.createElement("p");
        title.className = "font-semibold text-slate-400";
        title.textContent = "Fuentes";
        srcBox.appendChild(title);

        fuentes.slice(0, 5).forEach((s) => {
          const line = document.createElement("p");
          line.className = "mt-1";
          const name = s.documentoNombre || `Doc ${s.documentoId}`;
          line.textContent = `[Doc: ${name}, Fragmento ${s.indiceChunk}]`;
          srcBox.appendChild(line);
        });
        bubble.appendChild(srcBox);
      }

      wrap.appendChild(bubble);
      chatMessages.appendChild(wrap);
      chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    async function loadConversaciones(selectId = null) {
      const list = await apiFetch("/api/chat/conversaciones");
      const conv = (Array.isArray(list) ? list : []).filter((c) => Number(c.asignaturaId) === notebookId);
      conv.sort((a, b) => (b.updatedAt || "").localeCompare(a.updatedAt || ""));

      if (!conversationSelect) return conv;
      conversationSelect.innerHTML = "";

      if (!conv.length) {
        const opt = document.createElement("option");
        opt.value = "";
        opt.textContent = "Sin conversaciones";
        conversationSelect.appendChild(opt);
        currentConversationId = null;
        return conv;
      }

      conv.forEach((c) => {
        const opt = document.createElement("option");
        opt.value = String(c.id);
        opt.textContent = c.titulo || `Conversación #${c.id}`;
        conversationSelect.appendChild(opt);
      });

      const toSelect = selectId ? String(selectId) : String(conv[0].id);
      conversationSelect.value = toSelect;
      currentConversationId = Number(toSelect);
      await loadMensajes();
      return conv;
    }

    async function loadMensajes() {
      if (!currentConversationId || !chatMessages) return;
      chatMessages.innerHTML = "";
      const msgs = await apiFetch(`/api/chat/conversaciones/${currentConversationId}/mensajes`);
      (Array.isArray(msgs) ? msgs : []).forEach((m) => renderChatMessage(m));
    }

    conversationSelect?.addEventListener("change", async () => {
      const id = Number(conversationSelect.value);
      currentConversationId = id || null;
      await loadMensajes();
    });

    async function createConversacion() {
      const close = openModal(
        "Nueva conversación",
        `
        <form id="new-conv-form" class="space-y-3">
          <div>
            <label class="block text-xs font-medium text-slate-400">Título</label>
            <input id="conv-title" class="mt-1.5 w-full rounded-xl border border-slate-800 bg-slate-950/20 px-3 py-2 text-sm text-slate-200 outline-none focus:border-brand-500" placeholder="Ej: Dudas del tema" />
          </div>
          <button type="submit" class="w-full rounded-xl bg-brand-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-brand-500">Crear</button>
        </form>
      `
      );

      const form = qs("new-conv-form");
      if (!form) return;
      form.addEventListener("submit", async (e) => {
        e.preventDefault();
        const titulo = qs("conv-title")?.value?.trim() || null;
        try {
          const created = await apiFetch("/api/chat/conversaciones", {
            method: "POST",
            body: { asignaturaId: notebookId, titulo },
          });
          close();
          toast("Conversación creada", "success");
          await loadConversaciones(created.id);
        } catch (err) {
          toast(err.message || "No se pudo crear la conversación", "error");
        }
      });
    }

    newConversationBtn?.addEventListener("click", createConversacion);

    chatForm?.addEventListener("submit", async (e) => {
      e.preventDefault();
      const content = chatInput?.value?.trim();
      if (!content) return;

      try {
        if (!currentConversationId) {
          const created = await apiFetch("/api/chat/conversaciones", {
            method: "POST",
            body: { asignaturaId: notebookId, titulo: "Conversación" },
          });
          currentConversationId = created.id;
          await loadConversaciones(currentConversationId);
        }

        chatInput.value = "";
        renderChatMessage({ rol: "USER", contenido: content });

        const res = await apiFetch(`/api/chat/conversaciones/${currentConversationId}/mensajes`, {
          method: "POST",
          body: { contenido: content },
        });

        if (res?.assistantMessage) renderChatMessage(res.assistantMessage, res.assistantMessage.fuentes);
      } catch (err) {
        toast(err.message || "Error enviando mensaje", "error");
      }
    });

    async function loadDocumentoDetalle(documentoId) {
      if (!documentoId) return;
      try {
        const detail = await apiFetch(`/api/documentos/${documentoId}/detalle?size=8`);
        const doc = detail.documento;

        if (docTitle) docTitle.textContent = doc.nombreOriginal || `Documento ${doc.id}`;
        if (docStatus) docStatus.textContent = `Estado: ${doc.estadoProcesado} · Tema: ${doc.temaId ?? "—"}`;

        const estado = fmtEstadoDocumento(doc.estadoProcesado);
        docStatus?.classList.remove("text-slate-400", "text-rose-200", "text-emerald-200", "text-amber-200");
        docStatus?.classList.add("text-slate-300");

        if (docError) {
          if (doc.errorExtraccion) {
            docError.classList.remove("hidden");
            docError.textContent = doc.errorExtraccion;
          } else {
            docError.classList.add("hidden");
            docError.textContent = "";
          }
        }

        if (docChunks) {
          docChunks.innerHTML = "";
          const items = detail.chunks?.items || [];
          if (!items.length) {
            const p = document.createElement("p");
            p.className = "text-xs text-slate-500";
            p.textContent = "Sin chunks todavía.";
            docChunks.appendChild(p);
          } else {
            items.slice(0, 6).forEach((c) => {
              const div = document.createElement("div");
              div.className = "rounded-xl border border-slate-800 bg-slate-950/20 p-3";
              const meta = document.createElement("p");
              meta.className = "text-[11px] font-semibold text-slate-400";
              meta.textContent = `Fragmento ${c.indiceChunk}` + (c.paginaOrigen != null ? ` · Página ${c.paginaOrigen}` : "");
              const text = document.createElement("p");
              text.className = "mt-1 text-xs text-slate-200 whitespace-pre-wrap";
              text.textContent = truncate(c.texto, 260);
              div.appendChild(meta);
              div.appendChild(text);
              docChunks.appendChild(div);
            });
          }
        }

        if (docArtefacts) {
          docArtefacts.innerHTML = "";
          const res = Array.isArray(detail.resumenes) ? detail.resumenes : [];
          const fc = Array.isArray(detail.flashcards) ? detail.flashcards : [];
          const pq = Array.isArray(detail.preguntas) ? detail.preguntas : [];

          const line1 = document.createElement("p");
          line1.className = "text-sm text-slate-200";
          line1.textContent = `${res.length} resúmenes · ${fc.length} flashcards · ${pq.length} preguntas`;
          docArtefacts.appendChild(line1);

          if (res.length) {
            const last = res[0];
            const box = document.createElement("div");
            box.className = "mt-3 rounded-xl border border-slate-800 bg-slate-950/20 p-3";
            const t = document.createElement("p");
            t.className = "text-sm font-semibold";
            t.textContent = truncate(last.titulo || "Resumen", 90);
            const c = document.createElement("p");
            c.className = "mt-1 text-xs text-slate-400";
            c.textContent = truncate(last.contenido || "", 150);
            box.appendChild(t);
            box.appendChild(c);
            docArtefacts.appendChild(box);
          }
        }

        if (startFlashcards) startFlashcards.href = `/app/estudio/flashcards?documentoId=${documentoId}`;
        if (startTest) startTest.href = `/app/estudio/test?documentoId=${documentoId}`;

        genResumenBtn?.addEventListener("click", () => generateArtefact("resumen", documentoId));
        genFlashcardsBtn?.addEventListener("click", () => generateArtefact("flashcards", documentoId));
        genTestBtn?.addEventListener("click", () => generateArtefact("test", documentoId));
      } catch (err) {
        toast(err.message || "No se pudo cargar el documento", "error");
      }
    }

    async function generateArtefact(type, documentoId) {
      if (!documentoId) return;
      try {
        if (type === "resumen") {
          await apiFetch(`/api/resumen/generar/documento/${documentoId}`, { method: "POST" });
          toast("Resumen generado", "success");
        } else if (type === "flashcards") {
          await apiFetch(`/api/flashcards/generar/${documentoId}`, { method: "POST" });
          toast("Flashcards generadas", "success");
        } else if (type === "test") {
          await apiFetch(`/api/preguntas/generar/${documentoId}`, { method: "POST" });
          toast("Test generado", "success");
        }

        await loadDocumentoDetalle(documentoId);
        await refreshOverview();
      } catch (err) {
        toast(err.message || "No se pudo generar", "error");
      }
    }

    async function loadProgress() {
      try {
        const p = await apiFetch("/api/progreso/usuario");
        if (!progressBox) return;
        progressBox.innerHTML = "";
        const row1 = document.createElement("p");
        row1.textContent = `Flashcards repasadas: ${p.flashcardsRepasadas ?? 0}`;
        const row2 = document.createElement("p");
        row2.className = "mt-1";
        row2.textContent = `Tests completados: ${p.testsCompletados ?? 0}`;
        progressBox.appendChild(row1);
        progressBox.appendChild(row2);
      } catch {
        // optional
      }
    }

    try {
      await refreshOverview();
      await loadConversaciones();
      await loadProgress();
    } catch (err) {
      toast(err.message || "Error cargando notebook", "error");
    }
  }

  async function initNotebookSources() {
    requireAuth();
    setupLogout();

    const notebookId = Number(document.body.dataset.notebookId);
    if (!notebookId) {
      toast("Notebook inválido", "error");
      return;
    }

    const notebookTitle = qs("notebook-title");
    const uploadForm = qs("upload-form");
    const uploadFile = qs("upload-file");
    const uploadTema = qs("upload-tema");
    const temasList = qs("temas-list");
    const createTemaForm = qs("create-tema-form");
    const createTemaTitle = qs("create-tema-title");
    const docsList = qs("docs-list");
    const docsEmpty = qs("docs-empty");
    const refreshDocsBtn = qs("refresh-docs");

    let temas = [];
    let docs = [];

    function renderTemas() {
      if (!temasList) return;
      temasList.innerHTML = "";

      if (!Array.isArray(temas) || !temas.length) {
        const p = document.createElement("p");
        p.className = "text-sm text-slate-500";
        p.textContent = "Sin temas todavía.";
        temasList.appendChild(p);
        return;
      }

      temas.forEach((t) => {
        const row = document.createElement("div");
        row.className = "flex items-center justify-between gap-3 rounded-xl border border-slate-800 bg-slate-950/20 px-3 py-2";
        const left = document.createElement("div");
        const title = document.createElement("p");
        title.className = "text-sm font-semibold";
        title.textContent = t.titulo || `Tema #${t.id}`;
        const meta = document.createElement("p");
        meta.className = "text-[11px] text-slate-500";
        meta.textContent = `ID: ${t.id}`;
        left.appendChild(title);
        left.appendChild(meta);
        row.appendChild(left);
        temasList.appendChild(row);
      });
    }

    function fillTemaSelect() {
      if (!uploadTema) return;
      const current = uploadTema.value || "";
      uploadTema.innerHTML = `<option value="">Sin tema</option>`;
      temas.forEach((t) => {
        const opt = document.createElement("option");
        opt.value = String(t.id);
        opt.textContent = t.titulo || `Tema #${t.id}`;
        uploadTema.appendChild(opt);
      });
      uploadTema.value = current;
    }

    function renderDocs() {
      if (!docsList) return;
      docsList.innerHTML = "";
      docsEmpty?.classList.add("hidden");

      if (!Array.isArray(docs) || !docs.length) {
        docsEmpty?.classList.remove("hidden");
        return;
      }

      docs.forEach((d) => {
        const estado = fmtEstadoDocumento(d.estadoProcesado);
        const card = document.createElement("div");
        card.className = "rounded-2xl border border-slate-800 bg-slate-950/20 p-4";

        const top = document.createElement("div");
        top.className = "flex items-start justify-between gap-3";

        const left = document.createElement("div");
        const title = document.createElement("p");
        title.className = "text-sm font-semibold";
        title.textContent = d.nombreOriginal || `Documento ${d.id}`;
        const meta = document.createElement("p");
        meta.className = "mt-1 text-xs text-slate-500";
        meta.textContent = d.temaId ? `Tema #${d.temaId}` : "Sin tema";
        left.appendChild(title);
        left.appendChild(meta);

        const badge = document.createElement("span");
        badge.className = `inline-flex items-center rounded-full border px-2 py-1 text-[10px] font-semibold ${estado.cls}`;
        badge.textContent = estado.label;

        top.appendChild(left);
        top.appendChild(badge);
        card.appendChild(top);

        if (d.errorExtraccion) {
          const err = document.createElement("div");
          err.className = "mt-3 rounded-xl border border-rose-900/50 bg-rose-950/30 p-3 text-xs text-rose-200";
          err.textContent = truncate(d.errorExtraccion, 220);
          card.appendChild(err);
        }

        const actions = document.createElement("div");
        actions.className = "mt-4 flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between";

        const link = document.createElement("a");
        link.href = `/app/notebooks/${notebookId}`;
        link.className = "text-xs font-semibold text-brand-400 hover:text-brand-300";
        link.textContent = "Abrir en notebook";

        const right = document.createElement("div");
        right.className = "flex items-center gap-2";

        const sel = document.createElement("select");
        sel.className = "rounded-xl border border-slate-800 bg-slate-950/20 px-3 py-2 text-xs text-slate-200 outline-none focus:border-brand-500";
        const optNone = document.createElement("option");
        optNone.value = "";
        optNone.textContent = "Sin tema";
        sel.appendChild(optNone);
        temas.forEach((t) => {
          const opt = document.createElement("option");
          opt.value = String(t.id);
          opt.textContent = t.titulo || `Tema #${t.id}`;
          sel.appendChild(opt);
        });
        sel.value = d.temaId ? String(d.temaId) : "";

        sel.addEventListener("change", async () => {
          const temaId = sel.value ? Number(sel.value) : null;
          try {
            const actual = await apiFetch(`/api/documentos/${d.id}`);
            await apiFetch(`/api/documentos/${d.id}`, {
              method: "PUT",
              body: {
                usuarioId: actual.usuarioId,
                asignaturaId: actual.asignaturaId,
                temaId,
                nombreOriginal: actual.nombreOriginal,
                rutaArchivo: actual.rutaArchivo,
                mimeType: actual.mimeType,
                extension: actual.extension,
                tamanoBytes: actual.tamanoBytes,
                checksumSha256: actual.checksumSha256,
                paginas: actual.paginas,
              },
            });
            toast("Tema actualizado", "success");
            await refreshDocs();
          } catch (err) {
            toast(err.message || "No se pudo asignar el tema", "error");
            sel.value = d.temaId ? String(d.temaId) : "";
          }
        });

        const del = document.createElement("button");
        del.className = "rounded-xl border border-slate-800 bg-slate-950/20 px-3 py-2 text-xs font-medium text-slate-200 hover:border-rose-500";
        del.textContent = "Eliminar";
        del.addEventListener("click", async () => {
          if (!confirm("¿Eliminar este documento?")) return;
          try {
            await apiFetch(`/api/documentos/${d.id}`, { method: "DELETE" });
            toast("Documento eliminado", "success");
            await refreshDocs();
          } catch (err) {
            toast(err.message || "No se pudo eliminar", "error");
          }
        });

        right.appendChild(sel);
        right.appendChild(del);
        actions.appendChild(link);
        actions.appendChild(right);
        card.appendChild(actions);

        docsList.appendChild(card);
      });
    }

    async function refreshTemas() {
      try {
        temas = await apiFetch(`/api/temas?asignaturaId=${notebookId}`);
      } catch {
        temas = [];
      }
      renderTemas();
      fillTemaSelect();
    }

    async function refreshDocs() {
      try {
        const overview = await apiFetch(`/api/notebooks/${notebookId}/overview`);
        notebookTitle.textContent = overview?.notebook?.nombre || `Notebook ${notebookId}`;
        docs = Array.isArray(overview.documentos) ? overview.documentos : [];
      } catch (err) {
        toast(err.message || "No se pudieron cargar documentos", "error");
        docs = [];
      }
      renderDocs();
    }

    uploadForm?.addEventListener("submit", async (e) => {
      e.preventDefault();
      const file = uploadFile?.files?.[0];
      if (!file) return;
      const temaId = uploadTema?.value ? uploadTema.value : "";

      const fd = new FormData();
      fd.append("file", file);
      fd.append("asignaturaId", String(notebookId));
      if (temaId) fd.append("temaId", temaId);

      try {
        const res = await apiFetch("/api/documentos/upload", { method: "POST", body: fd });
        const warnings = res?.warnings || [];
        toast(warnings.length ? "Subido con avisos" : "Documento subido", warnings.length ? "info" : "success");
        if (warnings.length) warnings.forEach((w) => toast(w, "info"));
        uploadFile.value = "";
        await refreshDocs();
      } catch (err) {
        toast(err.message || "No se pudo subir el documento", "error");
      }
    });

    createTemaForm?.addEventListener("submit", async (e) => {
      e.preventDefault();
      const titulo = createTemaTitle?.value?.trim();
      if (!titulo) return;
      try {
        await apiFetch("/api/temas", { method: "POST", body: { asignaturaId: notebookId, titulo } });
        createTemaTitle.value = "";
        toast("Tema creado", "success");
        await refreshTemas();
      } catch (err) {
        toast(err.message || "No se pudo crear el tema", "error");
      }
    });

    refreshDocsBtn?.addEventListener("click", refreshDocs);

    await refreshDocs();
    await refreshTemas();
  }

  async function initChatPage() {
    requireAuth();
    setupLogout();

    const listEl = qs("conversation-list");
    const newBtn = qs("new-conversation-btn");
    const chatMessages = qs("chat-messages");
    const chatForm = qs("chat-form");
    const chatInput = qs("chat-input");

    let conversations = [];
    let activeId = null;

    function renderConversations() {
      if (!listEl) return;
      listEl.innerHTML = "";
      if (!conversations.length) {
        const p = document.createElement("p");
        p.className = "text-sm text-slate-500";
        p.textContent = "No hay conversaciones.";
        listEl.appendChild(p);
        return;
      }

      conversations.forEach((c) => {
        const btn = document.createElement("button");
        btn.className =
          "w-full rounded-xl border border-slate-800 bg-slate-950/20 px-3 py-2 text-left hover:border-brand-500";
        if (c.id === activeId) btn.classList.add("border-brand-500");
        btn.addEventListener("click", async () => {
          activeId = c.id;
          renderConversations();
          await loadMensajes();
        });
        const t = document.createElement("p");
        t.className = "text-sm font-semibold";
        t.textContent = c.titulo || `Conversación #${c.id}`;
        const meta = document.createElement("p");
        meta.className = "mt-0.5 text-xs text-slate-500";
        meta.textContent = c.asignaturaId ? `Notebook #${c.asignaturaId}` : "Sin notebook";
        btn.appendChild(t);
        btn.appendChild(meta);
        listEl.appendChild(btn);
      });
    }

    function renderChatMessage(msg) {
      if (!chatMessages) return;
      const isUser = (msg.rol || "").toUpperCase() === "USER";
      const wrap = document.createElement("div");
      wrap.className = `flex ${isUser ? "justify-end" : "justify-start"}`;

      const bubble = document.createElement("div");
      bubble.className = isUser
        ? "max-w-[85%] rounded-2xl bg-brand-600/20 px-4 py-3 text-sm text-slate-100"
        : "max-w-[85%] rounded-2xl border border-slate-800 bg-slate-950/20 px-4 py-3 text-sm text-slate-100";
      bubble.textContent = msg.contenido || "";
      wrap.appendChild(bubble);
      chatMessages.appendChild(wrap);
      chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    async function loadMensajes() {
      if (!activeId || !chatMessages) return;
      chatMessages.innerHTML = "";
      const msgs = await apiFetch(`/api/chat/conversaciones/${activeId}/mensajes`);
      (Array.isArray(msgs) ? msgs : []).forEach((m) => renderChatMessage(m));
    }

    async function refresh() {
      conversations = await apiFetch("/api/chat/conversaciones");
      conversations = Array.isArray(conversations) ? conversations : [];
      conversations.sort((a, b) => (b.updatedAt || "").localeCompare(a.updatedAt || ""));
      activeId = conversations[0]?.id || null;
      renderConversations();
      await loadMensajes();
    }

    newBtn?.addEventListener("click", () => {
      const close = openModal(
        "Nueva conversación",
        `
        <form id="chat-new-form" class="space-y-3">
          <div>
            <label class="block text-xs font-medium text-slate-400">Notebook ID (asignaturaId)</label>
            <input id="chat-asignatura-id" type="number" class="mt-1.5 w-full rounded-xl border border-slate-800 bg-slate-950/20 px-3 py-2 text-sm text-slate-200 outline-none focus:border-brand-500" placeholder="Opcional" />
          </div>
          <div>
            <label class="block text-xs font-medium text-slate-400">Título</label>
            <input id="chat-title" class="mt-1.5 w-full rounded-xl border border-slate-800 bg-slate-950/20 px-3 py-2 text-sm text-slate-200 outline-none focus:border-brand-500" placeholder="Ej: Dudas" />
          </div>
          <button type="submit" class="w-full rounded-xl bg-brand-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-brand-500">Crear</button>
        </form>
      `
      );

      const form = qs("chat-new-form");
      form?.addEventListener("submit", async (e) => {
        e.preventDefault();
        const asignaturaId = Number(qs("chat-asignatura-id")?.value) || null;
        const titulo = qs("chat-title")?.value?.trim() || null;
        try {
          const created = await apiFetch("/api/chat/conversaciones", {
            method: "POST",
            body: { asignaturaId, titulo },
          });
          close();
          toast("Conversación creada", "success");
          await refresh();
          activeId = created.id;
          renderConversations();
          await loadMensajes();
        } catch (err) {
          toast(err.message || "No se pudo crear", "error");
        }
      });
    });

    chatForm?.addEventListener("submit", async (e) => {
      e.preventDefault();
      const content = chatInput?.value?.trim();
      if (!content || !activeId) return;
      chatInput.value = "";
      renderChatMessage({ rol: "USER", contenido: content });
      try {
        const res = await apiFetch(`/api/chat/conversaciones/${activeId}/mensajes`, {
          method: "POST",
          body: { contenido: content },
        });
        if (res?.assistantMessage) renderChatMessage(res.assistantMessage);
      } catch (err) {
        toast(err.message || "Error enviando", "error");
      }
    });

    await refresh();
  }

  async function initStudyFlashcards() {
    requireAuth();
    setupLogout();

    const params = new URLSearchParams(window.location.search);
    const documentoId = params.get("documentoId");
    const temaId = params.get("temaId");

    const counter = qs("flashcard-counter");
    const qEl = qs("flashcard-question");
    const answerBox = qs("flashcard-answer-box");
    const ansEl = qs("flashcard-answer");
    const empty = qs("study-empty");

    const toggleBtn = qs("toggle-answer-btn");
    const knownBtn = qs("known-btn");
    const nextBtn = qs("next-btn");
    const shuffleBtn = qs("shuffle-btn");

    let flashcards = [];
    let idx = 0;
    let shown = false;

    function render() {
      if (!flashcards.length) {
        empty?.classList.remove("hidden");
        counter.textContent = "—";
        qEl.textContent = "—";
        ansEl.textContent = "—";
        answerBox?.classList.add("hidden");
        return;
      }
      empty?.classList.add("hidden");

      const fc = flashcards[idx];
      counter.textContent = `${idx + 1} / ${flashcards.length} · Dificultad ${fc.dificultad ?? 3}`;
      qEl.textContent = fc.pregunta || "—";
      ansEl.textContent = fc.respuesta || "—";
      answerBox?.classList.toggle("hidden", !shown);
      toggleBtn.textContent = shown ? "Ocultar respuesta" : "Ver respuesta";
    }

    function next() {
      if (!flashcards.length) return;
      shown = false;
      idx = (idx + 1) % flashcards.length;
      render();
    }

    toggleBtn?.addEventListener("click", () => {
      if (!flashcards.length) return;
      shown = !shown;
      render();
    });

    nextBtn?.addEventListener("click", next);

    shuffleBtn?.addEventListener("click", () => {
      flashcards = flashcards
        .map((x) => ({ x, r: Math.random() }))
        .sort((a, b) => a.r - b.r)
        .map((o) => o.x);
      idx = 0;
      shown = false;
      toast("Flashcards mezcladas", "info");
      render();
    });

    knownBtn?.addEventListener("click", async () => {
      if (!flashcards.length) return;
      const fc = flashcards[idx];
      try {
        await apiFetch(`/api/flashcards/${fc.id}/repasar`, { method: "POST" });
      } catch {
        // optional
      }
      next();
    });

    try {
      if (documentoId) {
        flashcards = await apiFetch(`/api/flashcards/documento/${documentoId}`);
      } else if (temaId) {
        flashcards = await apiFetch(`/api/flashcards/tema/${temaId}`);
      } else {
        flashcards = [];
      }
      flashcards = Array.isArray(flashcards) ? flashcards : [];
      idx = 0;
      render();
    } catch (err) {
      toast(err.message || "No se pudieron cargar flashcards", "error");
      flashcards = [];
      render();
    }
  }

  async function initStudyTest() {
    requireAuth();
    setupLogout();

    const params = new URLSearchParams(window.location.search);
    const documentoId = params.get("documentoId");
    const temaId = params.get("temaId");

    const counter = qs("test-counter");
    const qText = qs("question-text");
    const optionsBox = qs("options-box");
    const feedback = qs("answer-feedback");
    const empty = qs("test-empty");
    const nextBtn = qs("next-question-btn");
    const restartBtn = qs("restart-test-btn");

    let preguntas = [];
    let idx = 0;
    let answered = false;

    function render() {
      if (!preguntas.length) {
        empty?.classList.remove("hidden");
        counter.textContent = "—";
        qText.textContent = "—";
        optionsBox.innerHTML = "";
        feedback?.classList.add("hidden");
        return;
      }
      empty?.classList.add("hidden");

      const p = preguntas[idx];
      counter.textContent = `${idx + 1} / ${preguntas.length} · Dificultad ${p.dificultad ?? 3}`;
      qText.textContent = p.enunciado || "—";
      optionsBox.innerHTML = "";
      feedback?.classList.add("hidden");
      feedback.textContent = "";
      answered = false;

      const opciones = Array.isArray(p.opciones) ? p.opciones : [];
      opciones.forEach((o, i) => {
        const btn = document.createElement("button");
        btn.className =
          "w-full rounded-xl border border-slate-800 bg-slate-950/20 px-3 py-2 text-left text-sm text-slate-200 hover:border-brand-500 disabled:opacity-60";
        btn.textContent = o.texto || `Opción ${i + 1}`;
        btn.addEventListener("click", async () => {
          if (answered) return;
          answered = true;
          try {
            const res = await apiFetch(`/api/preguntas/${p.id}/responder`, {
              method: "POST",
              body: { indiceOpcion: i },
            });
            const correcta = !!res.correcta;
            const indiceCorrecto = res.indiceCorrecto;

            Array.from(optionsBox.children).forEach((child, idx2) => {
              child.disabled = true;
              if (idx2 === indiceCorrecto) {
                child.classList.add("border-emerald-600");
              }
              if (idx2 === i && !correcta) {
                child.classList.add("border-rose-600");
              }
            });

            feedback.classList.remove("hidden");
            feedback.className =
              "mt-4 rounded-xl border px-3 py-3 text-sm " +
              (correcta
                ? "border-emerald-900/60 bg-emerald-950/30 text-emerald-100"
                : "border-rose-900/60 bg-rose-950/30 text-rose-100");
            feedback.textContent = correcta ? "Correcto." : "Incorrecto.";
            if (res.explicacion) {
              const extra = document.createElement("p");
              extra.className = "mt-2 text-xs text-slate-200/90";
              extra.textContent = res.explicacion;
              feedback.appendChild(extra);
            }
          } catch (err) {
            answered = false;
            toast(err.message || "No se pudo responder", "error");
          }
        });

        optionsBox.appendChild(btn);
      });
    }

    function next() {
      if (!preguntas.length) return;
      idx = (idx + 1) % preguntas.length;
      render();
    }

    nextBtn?.addEventListener("click", next);
    restartBtn?.addEventListener("click", () => {
      idx = 0;
      render();
    });

    try {
      if (documentoId) {
        preguntas = await apiFetch(`/api/preguntas/documento/${documentoId}`);
      } else if (temaId) {
        preguntas = await apiFetch(`/api/preguntas/tema/${temaId}`);
      } else {
        preguntas = [];
      }
      preguntas = Array.isArray(preguntas) ? preguntas : [];
      idx = 0;
      render();
    } catch (err) {
      toast(err.message || "No se pudieron cargar preguntas", "error");
      preguntas = [];
      render();
    }
  }

  async function initNotes() {
    requireAuth();
    setupLogout();

    const form = qs("create-note-form");
    const refreshBtn = qs("refresh-notes");
    const grid = qs("notes-grid");
    const empty = qs("notes-empty");

    const params = new URLSearchParams(window.location.search);
    const documentoId = params.get("documentoId");
    const temaId = params.get("temaId");
    const asignaturaId = params.get("asignaturaId");

    if (documentoId) qs("note-documento-id").value = documentoId;
    if (temaId) qs("note-tema-id").value = temaId;

    async function refresh() {
      if (!grid) return;
      grid.innerHTML = "";
      empty?.classList.add("hidden");

      const qsParams = new URLSearchParams();
      if (documentoId) qsParams.set("documentoId", documentoId);
      if (temaId) qsParams.set("temaId", temaId);
      if (asignaturaId) qsParams.set("asignaturaId", asignaturaId);

      try {
        const notas = await apiFetch(`/api/notas?${qsParams.toString()}`);
        const items = Array.isArray(notas) ? notas : [];
        if (!items.length) {
          empty?.classList.remove("hidden");
          return;
        }

        items.forEach((n) => {
          const card = document.createElement("div");
          card.className = "rounded-2xl border border-slate-800 bg-slate-950/20 p-4";
          if (n.colorHex) card.style.borderColor = n.colorHex;

          const title = document.createElement("p");
          title.className = "text-sm font-semibold";
          title.textContent = n.titulo || "Nota";

          const content = document.createElement("p");
          content.className = "mt-2 text-xs text-slate-300 whitespace-pre-wrap";
          content.textContent = truncate(n.contenido || "", 260);

          const meta = document.createElement("p");
          meta.className = "mt-3 text-[11px] text-slate-500";
          meta.textContent = `Doc: ${n.documentoId ?? "—"} · Chunk: ${n.chunkId ?? "—"} · Tema: ${n.temaId ?? "—"}`;

          const del = document.createElement("button");
          del.className = "mt-4 w-full rounded-xl border border-slate-800 bg-slate-950/10 px-3 py-2 text-xs font-medium text-slate-200 hover:border-rose-500";
          del.textContent = "Eliminar";
          del.addEventListener("click", async () => {
            if (!confirm("¿Eliminar esta nota?")) return;
            try {
              await apiFetch(`/api/notas/${n.id}`, { method: "DELETE" });
              toast("Nota eliminada", "success");
              await refresh();
            } catch (err) {
              toast(err.message || "No se pudo eliminar", "error");
            }
          });

          card.appendChild(title);
          card.appendChild(content);
          card.appendChild(meta);
          card.appendChild(del);
          grid.appendChild(card);
        });
      } catch (err) {
        toast(err.message || "No se pudieron cargar notas", "error");
      }
    }

    form?.addEventListener("submit", async (e) => {
      e.preventDefault();

      const titulo = qs("note-title")?.value?.trim();
      const contenido = qs("note-content")?.value?.trim();
      const colorHex = qs("note-color")?.value?.trim() || null;
      const docIdVal = Number(qs("note-documento-id")?.value) || null;
      const chunkIdVal = Number(qs("note-chunk-id")?.value) || null;
      const temaIdVal = Number(qs("note-tema-id")?.value) || null;

      if (!titulo || !contenido) return;

      try {
        await apiFetch("/api/notas", {
          method: "POST",
          body: {
            documentoId: docIdVal,
            chunkId: chunkIdVal,
            temaId: temaIdVal,
            titulo,
            contenido,
            colorHex,
          },
        });

        qs("note-title").value = "";
        qs("note-content").value = "";
        toast("Nota guardada", "success");
        await refresh();
      } catch (err) {
        toast(err.message || "No se pudo guardar", "error");
      }
    });

    refreshBtn?.addEventListener("click", refresh);

    await refresh();
  }

  async function init() {
    const page = document.body.dataset.page;
    if (!page) return;

    if (page !== "login") requireAuth();

    if (page === "login") return initLogin();
    if (page === "notebook-index") return initNotebookIndex();
    if (page === "notebook-detail") return initNotebookDetail();
    if (page === "notebook-sources") return initNotebookSources();
    if (page === "chat") return initChatPage();
    if (page === "study-flashcards") return initStudyFlashcards();
    if (page === "study-test") return initStudyTest();
    if (page === "notes") return initNotes();
  }

  document.addEventListener("DOMContentLoaded", init);
})();

