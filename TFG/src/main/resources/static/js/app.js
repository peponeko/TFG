(() => {
  const STORAGE_KEY = "easy4you.auth";
  const TOKEN_COOKIE = "easy4you_token";
  const NIVEL_ESTUDIO_KEY = "easy4you.nivel_estudio";

  function nivelEstudioKey() {
    const userId = getAuth()?.userId;
    return userId ? `${NIVEL_ESTUDIO_KEY}.${userId}` : NIVEL_ESTUDIO_KEY;
  }

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

  function setTokenCookie(token) {
    if (!token) return;
    document.cookie = `${TOKEN_COOKIE}=${token}; Path=/; SameSite=Lax`;
  }

  function clearTokenCookie() {
    document.cookie = `${TOKEN_COOKIE}=; Path=/; Max-Age=0; SameSite=Lax`;
  }

  function setAuth(auth) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
    if (auth?.token) setTokenCookie(auth.token);
  }

  function clearAuth() {
    localStorage.removeItem(STORAGE_KEY);
    clearTokenCookie();
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
      info: "border-[#cfe3ff] bg-[#eef5ff] text-[#1d4e89]",
      success: "border-[#b9e7cf] bg-[#eafaf1] text-[#0f5132]",
      warning: "border-[#ffe2a8] bg-[#fff7e8] text-[#8a5a00]",
      error: "border-[#ffc4cb] bg-[#fff1f3] text-[#8b1e2d]",
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
      <div class="apple-modal-overlay">
        <div class="apple-modal" style="max-width: 520px;">
          <div class="flex items-center justify-between gap-3" style="border-bottom:1px solid var(--apple-separator); padding-bottom:12px;">
            <h2 class="text-sm font-semibold" style="color: var(--apple-text);">${title}</h2>
            <button id="e4y-modal-close" class="apple-btn-secondary" style="padding:8px 10px; font-size:12px;">Cerrar</button>
          </div>
          <div style="padding-top:14px;">${bodyHtml}</div>
        </div>
      </div>
    `;

    const close = () => {
      root.innerHTML = "";
      root.classList.add("hidden");
    };

    const closeBtn = qs("e4y-modal-close");
    if (closeBtn) closeBtn.addEventListener("click", close);
    root.querySelector(".apple-modal-overlay")?.addEventListener("click", (event) => {
      if (event.target === event.currentTarget) close();
    });

    return close;
  }

  function openResumenViewer(title, content) {
    const root = qs("modal-root");
    if (!root) return () => {};

    const safeTitle = title || "Resumen";
    const safeContent = content || "Sin contenido";

    root.classList.remove("hidden");
    root.innerHTML = `
      <div class="resumen-viewer-overlay">
        <div class="resumen-viewer">
          <div class="resumen-viewer-header">
            <h2 class="resumen-viewer-title">${safeTitle}</h2>
            <div style="display:flex; gap:8px;">
              <button id="resumen-viewer-copy" class="apple-btn-secondary" style="padding:8px 12px; font-size:12px;" title="Copiar contenido">Copiar</button>
              <button id="resumen-viewer-close" class="resumen-viewer-close" title="Cerrar">✕</button>
            </div>
          </div>
          <div class="resumen-viewer-content">
            <div>${safeContent}</div>
          </div>
        </div>
      </div>
    `;

    const close = () => {
      root.innerHTML = "";
      root.classList.add("hidden");
      document.removeEventListener("keydown", escHandler);
    };

    const escHandler = (e) => {
      if (e.key === "Escape") close();
    };

    qs("resumen-viewer-close")?.addEventListener("click", close);
    qs("resumen-viewer-copy")?.addEventListener("click", async () => {
      try {
        await navigator.clipboard.writeText(safeContent);
        toast("Resumen copiado al portapapeles", "success");
      } catch {
        toast("No se pudo copiar", "error");
      }
    });
    root.querySelector(".resumen-viewer-overlay")?.addEventListener("click", (event) => {
      if (event.target === event.currentTarget) close();
    });
    document.addEventListener("keydown", escHandler);

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
      window.location.href = "/app/login?expired=1";
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

  // Comprobación simple de expiración del JWT (exp en segundos).
  function checkTokenExpiry() {
    const token = getToken();
    if (!token) return;
    try {
      const parts = token.split(".");
      if (parts.length < 2) return;
      const payload = JSON.parse(atob(parts[1].replace(/-/g, "+").replace(/_/g, "/")));
      if (typeof payload?.exp === "number" && payload.exp * 1000 < Date.now()) {
        clearAuth();
        window.location.href = "/app/login?expired=1";
      }
    } catch {
      // token mal formado: lo ignoramos, el backend ya devolverá 401
    }
  }

  function estadoBadgeClass(estado) {
    const e = (estado || "").toUpperCase();
    if (e === "PROCESANDO") return "apple-chip apple-badge-processing";
    if (e === "PROCESADO" || e === "LISTO") return "apple-chip apple-badge-done";
    if (e === "ERROR") return "apple-chip apple-badge-error";
    return "apple-chip apple-badge-pending";
  }

  async function pollDocumentoEstado(documentoId, onDone, onError) {
    for (let i = 0; i < 60; i++) {
      try {
        const estado = await apiFetch(`/api/documentos/${documentoId}/estado`);
        const actual = (estado?.estadoProcesado || "").toUpperCase();
        if (actual === "LISTO" || actual === "PROCESADO") return onDone?.(estado);
        if (actual === "ERROR") return onError?.(estado?.error || "Error procesando el documento");
      } catch (err) {
        return onError?.(err.message || "No se pudo consultar el estado del documento");
      }
      await new Promise((resolve) => setTimeout(resolve, 3000));
    }
    onError?.("La generación está tardando mucho. Vuelve a intentarlo en unos segundos.");
  }

  window.addEventListener("unhandledrejection", (event) => {
    if (event?.reason?.status === 401) {
      clearAuth();
      window.location.href = "/app/login?expired=1";
    }
  });

  function fmtEstadoDocumento(estado) {
    const e = (estado || "").toUpperCase();
    if (e === "PROCESADO" || e === "LISTO") return { label: "Procesado", cls: "apple-chip apple-badge-done" };
    if (e === "PROCESANDO") return { label: "Procesando", cls: "apple-chip apple-badge-processing" };
    if (e === "ERROR") return { label: "Error", cls: "apple-chip apple-badge-error" };
    return { label: e || "Pendiente", cls: "apple-chip apple-badge-pending" };
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

  function getNivelEstudio() {
    const raw = localStorage.getItem(nivelEstudioKey());
    return raw ? String(raw).trim().toLowerCase() : null;
  }

  function isUniversitario() {
    return getNivelEstudio() === "universitario";
  }

  function etiquetaPeriodo(num) {
    if (num === 0) return "General";
    if (isUniversitario()) return `Cuatrimestre ${num}`;
    if (num === 1) return "Primer trimestre";
    if (num === 2) return "Segundo trimestre";
    if (num === 3) return "Tercer trimestre";
    return "General";
  }

  function parsePeriodoDesdeEtiqueta(texto) {
    const t = String(texto || "").trim().toLowerCase();
    if (!t) return 0;
    if (t.includes("1")) return 1;
    if (t.includes("2")) return 2;
    if (t.includes("3")) return 3;
    return 0;
  }

  async function ensureNivelEstudioSeleccionado() {
    const existente = getNivelEstudio();
    if (existente) return existente;

    return await new Promise((resolve) => {
      const close = openModal(
        "Antes de empezar",
        `
        <div style="display:flex; flex-direction:column; gap:10px;">
          <p style="margin:0; color: var(--apple-text-secondary);">¿Cuál es tu nivel educativo?</p>
          <button class="apple-btn-secondary nivel-opt" data-nivel="universitario">Estudiante universitario</button>
          <button class="apple-btn-secondary nivel-opt" data-nivel="ciclo-superior">Ciclo superior</button>
          <button class="apple-btn-secondary nivel-opt" data-nivel="ciclo-medio">Ciclo medio</button>
          <button class="apple-btn-secondary nivel-opt" data-nivel="eso-bachillerato">ESO o Bachillerato</button>
          <button class="apple-btn-secondary nivel-opt" data-nivel="primaria">Primaria</button>
          <button class="apple-btn-secondary nivel-opt" data-nivel="no-estudiante">No estudiante</button>
        </div>
      `
      );
      Array.from(document.querySelectorAll(".nivel-opt")).forEach((btn) => {
        btn.addEventListener("click", () => {
          const nivel = btn.dataset.nivel;
          localStorage.setItem(nivelEstudioKey(), nivel);
          close();
          resolve(nivel);
        });
      });
    });
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
      window.location.href = "/app/home";
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
          window.location.href = "/app/home";
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
          await apiFetch("/api/auth/register", {
            method: "POST",
            body: { nombre, apellidos, email, password },
          });
          registerForm.reset();
          toast("Cuenta creada con éxito. Inicia sesión para continuar.", "success");
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
            a.className = "apple-card";
            a.style.padding = "18px";
            a.style.border = "1px solid var(--apple-separator)";

            const badge = document.createElement("div");
            const color = normalizeHex(nb.colorHex) || "#6D4CFF";
            badge.className = "h-2 w-10 rounded-full";
            badge.style.background = color;

            const title = document.createElement("h3");
            title.className = "mt-4 text-lg font-semibold tracking-tight";
            title.textContent = nb.nombre || `Notebook ${nb.id}`;

            const desc = document.createElement("p");
            desc.className = "mt-1 text-sm";
            desc.style.color = "var(--apple-text-secondary)";
            desc.textContent = truncate(nb.descripcion || "Sin descripción", 120);

            const meta = document.createElement("p");
            meta.className = "mt-4 text-xs";
            meta.style.color = "var(--apple-text-secondary)";
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
          card.className = "apple-card";
          card.style.padding = "14px";
          card.style.border = "1px solid var(--apple-separator)";

          const title = document.createElement("p");
          title.className = "text-sm font-semibold";
          title.textContent = `Notebook #${s.asignaturaId}`;

          const meta = document.createElement("p");
          meta.className = "mt-1 text-xs";
          meta.style.color = "var(--apple-text-secondary)";
          meta.textContent = `Rol: ${s.rol || "VIEWER"} · Propietario: ${s.propietarioId}`;

          const hint = document.createElement("p");
          hint.className = "mt-3 text-xs";
          hint.style.color = "var(--apple-text-secondary)";
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
        <form id="create-notebook-form" style="display:flex; flex-direction:column; gap:10px;">
          <div>
            <label style="display:block; font-size:12px; color:var(--apple-text-secondary); margin-bottom:4px;">Nombre</label>
            <input id="nb-nombre" required class="apple-input" placeholder="Ej: Redes 1" />
          </div>
          <div>
            <label style="display:block; font-size:12px; color:var(--apple-text-secondary); margin-bottom:4px;">Descripción</label>
            <textarea id="nb-descripcion" rows="3" class="apple-input" placeholder="Opcional"></textarea>
          </div>
          <div>
            <label style="display:block; font-size:12px; color:var(--apple-text-secondary); margin-bottom:4px;">Color</label>
            <input id="nb-color" type="color" value="#0071e3" class="apple-input" style="height: 40px; padding: 4px;" />
          </div>
          <button type="submit" class="apple-btn-primary">Crear</button>
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

    const temaIdRaw = new URLSearchParams(window.location.search).get("temaId");
    const temaId = temaIdRaw != null && temaIdRaw !== "" ? Number(temaIdRaw) : null;
    const hasTemaId = temaId != null && Number.isFinite(temaId) && temaId > 0;

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
      const baseCls = "apple-btn-secondary";
      const baseStyle = "padding:8px 12px; font-size:12px;";
      if (tabChat) {
        tabChat.dataset.active = isChat ? "true" : "false";
        tabChat.className = baseCls;
        tabChat.setAttribute("style", baseStyle + (isChat ? "background:#1d1d1f; color:#fff;" : ""));
      }
      if (tabDoc) {
        tabDoc.className = baseCls;
        tabDoc.setAttribute("style", baseStyle + (!isChat ? "background:#1d1d1f; color:#fff;" : ""));
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
        p.className = "text-sm";
        p.style.color = "var(--apple-text-secondary)";
        p.textContent = "No hay documentos.";
        sourcesList.appendChild(p);
        return;
      }

      docs.forEach((d) => {
        const estado = fmtEstadoDocumento(d.estadoProcesado);
        const wrap = document.createElement("div");
        wrap.style.position = "relative";

        const btn = document.createElement("button");
        btn.className = "w-full rounded-xl px-3 py-2 text-left";
        btn.style.border = "1px solid var(--apple-separator)";
        btn.style.background = "#ffffff";
        btn.addEventListener("click", () => {
          currentDocId = d.id;
          loadDocumentoDetalle(d.id);
          setActiveTab("doc");
        });

        const row = document.createElement("div");
        row.className = "flex items-start justify-between gap-3";

        const left = document.createElement("div");
        const name = document.createElement("p");
        name.className = "text-sm font-semibold";
        name.style.color = "var(--apple-text)";
        name.textContent = truncate(d.nombreOriginal || `Documento ${d.id}`, 52);
        const meta = document.createElement("p");
        meta.className = "mt-0.5 text-xs";
        meta.style.color = "var(--apple-text-secondary)";
        meta.textContent = d.temaId ? `Tema #${d.temaId}` : "Sin tema";
        left.appendChild(name);
        left.appendChild(meta);

        const badge = document.createElement("span");
        badge.className = `inline-flex items-center rounded-full border px-2 py-1 text-[10px] font-semibold ${estado.cls}`;
        badge.textContent = estado.label;

        row.appendChild(left);
        row.appendChild(badge);
        btn.appendChild(row);

        const del = document.createElement("button");
        del.className = "resumen-delete-btn";
        del.title = "Eliminar documento";
        del.innerHTML = "🗑️";
        del.style.top = "8px";
        del.style.right = "8px";
        del.addEventListener("click", async (e) => {
          e.preventDefault();
          e.stopPropagation();
          if (!d.id) return;
          if (!confirm("¿Eliminar este documento? Se borrará para poder re-subirlo y re-procesarlo.")) return;
          try {
            await apiFetch(`/api/documentos/${d.id}`, { method: "DELETE" });
            toast("Documento eliminado", "success");
            await refreshOverview();
            currentDocId = null;
            docTitle.textContent = "—";
            docStatus.textContent = "—";
            docError?.classList.add("hidden");
          } catch (err) {
            toast(err.message || "No se pudo eliminar el documento", "error");
          }
        });

        wrap.appendChild(btn);
        wrap.appendChild(del);
        sourcesList.appendChild(wrap);
      });
    }

    sourcesSearch?.addEventListener("input", () => renderSources());

    function renderOverview(data) {
      notebookTitle.textContent = data?.notebook?.nombre || `Notebook ${notebookId}`;

      currentDocs = Array.isArray(data.documentos) ? data.documentos : [];
      renderSources();

      if (resumenesList) {
        resumenesList.innerHTML = "";
        const items = Array.isArray(data.resumenes) ? data.resumenes.slice(0, 5) : [];
        if (!items.length) {
          const p = document.createElement("p");
          p.style.fontSize = "13px";
          p.style.color = "var(--apple-text-secondary)";
          p.textContent = "Aún no hay resúmenes.";
          resumenesList.appendChild(p);
        } else {
          items.forEach((r) => {
            const el = document.createElement("div");
            el.className = "resumen-card";
            
            const t = document.createElement("p");
            t.className = "resumen-card-title";
            t.textContent = truncate(r.titulo || "Resumen", 80);
            
            const c = document.createElement("p");
            c.className = "resumen-card-preview";
            c.textContent = truncate(r.contenido || "", 120);

            const delBtn = document.createElement("button");
            delBtn.className = "resumen-delete-btn";
            delBtn.title = "Eliminar resumen";
            delBtn.innerHTML = "🗑️";
            delBtn.addEventListener("click", async (e) => {
              e.preventDefault();
              e.stopPropagation();
              if (!r.id) return;
              try {
                await apiFetch(`/api/resumen/${r.id}`, { method: "DELETE" });
                toast("Resumen eliminado");
                await refreshOverview();
              } catch (err) {
                toast("No se pudo eliminar el resumen", "error");
              }
            });
            
            const viewBtn = document.createElement("button");
            viewBtn.className = "resumen-view-btn";
            viewBtn.innerHTML = "📖 Ver resumen completo";
            viewBtn.addEventListener("click", (e) => {
              e.preventDefault();
              e.stopPropagation();
              openResumenViewer(r.titulo || "Resumen", r.contenido || "");
            });
            
            el.appendChild(delBtn);
            el.appendChild(t);
            el.appendChild(c);
            el.appendChild(viewBtn);
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
          p.style.fontSize = "13px";
          p.style.color = "var(--apple-text-secondary)";
          p.textContent = "Aún no hay notas.";
          notesList.appendChild(p);
        } else {
          items.forEach((n) => {
            const el = document.createElement("div");
            el.style.border = "1px solid var(--apple-separator)";
            el.style.borderRadius = "12px";
            el.style.padding = "10px 12px";
            el.style.background = "#ffffff";
            el.style.marginBottom = "8px";
            const t = document.createElement("p");
            t.style.fontSize = "13px";
            t.style.fontWeight = "600";
            t.textContent = truncate(n.titulo || "Nota", 70);
            const c = document.createElement("p");
            c.style.marginTop = "4px";
            c.style.fontSize = "11px";
            c.style.color = "var(--apple-text-secondary)";
            c.textContent = truncate(n.contenido || "", 110);
            el.appendChild(t);
            el.appendChild(c);
            notesList.appendChild(el);
          });
        }
      }
    }

    async function refreshOverview() {
      const qsTema = hasTemaId ? `?temaId=${encodeURIComponent(String(temaId))}` : "";
      const data = await apiFetch(`/api/notebooks/${notebookId}/overview${qsTema}`);
      renderOverview(data);
      return data;
    }

    function renderChatMessage(msg, sources = null) {
      if (!chatMessages) return;
      const isUser = (msg.rol || "").toUpperCase() === "USER";

      const wrap = document.createElement("div");
      wrap.className = `apple-msg ${isUser ? "user" : "assistant"}`;

      const bubble = document.createElement("div");
      bubble.className = "apple-bubble";
      bubble.textContent = msg.contenido || "";

      const fuentes = sources || msg.fuentes;
      if (!isUser && Array.isArray(fuentes) && fuentes.length) {
        const srcBox = document.createElement("div");
        srcBox.style.marginTop = "10px";
        srcBox.style.paddingTop = "8px";
        srcBox.style.borderTop = "1px solid var(--apple-separator)";
        srcBox.style.fontSize = "12px";
        srcBox.style.color = "var(--apple-text-secondary)";
        const title = document.createElement("p");
        title.style.fontWeight = "600";
        title.textContent = "Fuentes";
        srcBox.appendChild(title);

        fuentes.slice(0, 5).forEach((s) => {
          const line = document.createElement("p");
          line.style.marginTop = "4px";
          const name = s.documentoNombre || `Doc ${s.documentoId}`;
          line.textContent = `[Doc: ${name}]`;
          srcBox.appendChild(line);
        });
        bubble.appendChild(srcBox);
      }

      wrap.appendChild(bubble);
      chatMessages.appendChild(wrap);
      chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    function renderTypingIndicator() {
      if (!chatMessages) return null;
      const wrap = document.createElement("div");
      wrap.className = "apple-msg assistant";
      wrap.id = "chat-typing-indicator";
      wrap.innerHTML = `<div class="apple-bubble" style="color: var(--apple-text-secondary);">IA escribiendo<span class="typing-dots"></span></div>`;
      chatMessages.appendChild(wrap);
      chatMessages.scrollTop = chatMessages.scrollHeight;
      return wrap;
    }

    function renderInlineChatError(message) {
      if (!chatMessages) return;
      const wrap = document.createElement("div");
      wrap.className = "apple-msg assistant";
      const bubble = document.createElement("div");
      bubble.className = "apple-bubble";
      bubble.style.background = "#fff1f3";
      bubble.style.borderColor = "#ffc4cb";
      bubble.style.color = "#8b1e2d";
      bubble.textContent = message || "No se pudo obtener respuesta de la IA";
      wrap.appendChild(bubble);
      chatMessages.appendChild(wrap);
      chatMessages.scrollTop = chatMessages.scrollHeight;
    }

    async function loadConversaciones(selectId = null) {
      const list = await apiFetch("/api/chat/conversaciones");
      const conv = (Array.isArray(list) ? list : []).filter((c) => {
        if (hasTemaId) return Number(c.temaId) === temaId;
        return Number(c.asignaturaId) === notebookId;
      });
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

    function renderChatSuggestions() {
      if (!chatMessages) return;
      const suggestions = [
        "¿De qué va este documento?",
        "Resumen de los puntos clave",
        "Crea 5 preguntas de repaso",
      ];
      const wrap = document.createElement("div");
      wrap.style.display = "flex";
      wrap.style.flexDirection = "column";
      wrap.style.alignItems = "center";
      wrap.style.justifyContent = "center";
      wrap.style.padding = "32px 16px";
      wrap.style.color = "var(--apple-text-secondary)";
      wrap.style.height = "100%";

      const title = document.createElement("p");
      title.textContent = "Empieza tu conversación";
      title.style.fontSize = "15px";
      title.style.fontWeight = "600";
      title.style.color = "var(--apple-text)";
      wrap.appendChild(title);

      const sub = document.createElement("p");
      sub.textContent = "Prueba con una de estas preguntas:";
      sub.style.fontSize = "13px";
      sub.style.marginTop = "4px";
      wrap.appendChild(sub);

      const list = document.createElement("div");
      list.style.marginTop = "16px";
      list.style.display = "flex";
      list.style.flexDirection = "column";
      list.style.gap = "8px";
      list.style.maxWidth = "420px";
      list.style.width = "100%";

      suggestions.forEach((s) => {
        const btn = document.createElement("button");
        btn.type = "button";
        btn.className = "apple-btn-secondary";
        btn.style.fontSize = "13px";
        btn.textContent = s;
        btn.addEventListener("click", () => {
          if (chatInput) {
            chatInput.value = s;
            chatInput.focus();
          }
        });
        list.appendChild(btn);
      });
      wrap.appendChild(list);
      chatMessages.appendChild(wrap);
    }

    async function loadMensajes() {
      if (!currentConversationId || !chatMessages) return;
      chatMessages.innerHTML = "";
      const msgs = await apiFetch(`/api/chat/conversaciones/${currentConversationId}/mensajes`);
      const list = Array.isArray(msgs) ? msgs : [];
      if (!list.length) {
        renderChatSuggestions();
        return;
      }
      list.forEach((m) => renderChatMessage(m));
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
        <form id="new-conv-form" style="display:flex; flex-direction:column; gap:10px;">
          <div>
            <label style="display:block; font-size:12px; color:var(--apple-text-secondary); margin-bottom:4px;">Título</label>
            <input id="conv-title" class="apple-input" placeholder="Ej: Dudas del tema" />
          </div>
          <button type="submit" class="apple-btn-primary">Crear</button>
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
            body: hasTemaId ? { temaId, titulo } : { asignaturaId: notebookId, titulo },
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
            body: hasTemaId ? { temaId, titulo: "Conversación" } : { asignaturaId: notebookId, titulo: "Conversación" },
          });
          currentConversationId = created.id;
          await loadConversaciones(currentConversationId);
        }

        chatInput.value = "";
        if (chatMessages && chatMessages.children.length === 1 && chatMessages.firstChild?.style?.height === "100%") {
          chatMessages.innerHTML = "";
        }
        renderChatMessage({ rol: "USER", contenido: content });

        const typing = renderTypingIndicator();
        try {
          const res = await apiFetch(`/api/chat/conversaciones/${currentConversationId}/mensajes`, {
            method: "POST",
            body: { contenido: content },
          });
          typing?.remove();
          if (res?.assistantMessage) renderChatMessage(res.assistantMessage, res.assistantMessage.fuentes);
        } catch (err) {
          typing?.remove();
          renderInlineChatError(err.message || "La IA no está disponible en este momento.");
        }
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
        if (docStatus) {
          docStatus.innerHTML = "";
          const estadoEl = document.createElement("span");
          estadoEl.className = estadoBadgeClass(doc.estadoProcesado);
          estadoEl.textContent = (doc.estadoProcesado || "PENDIENTE").toUpperCase();
          const txt = document.createElement("span");
          txt.style.marginLeft = "8px";
          txt.style.fontSize = "12px";
          txt.style.color = "var(--apple-text-secondary)";
          txt.textContent = `Tema: ${doc.temaId ?? "—"}`;
          docStatus.appendChild(estadoEl);
          docStatus.appendChild(txt);
        }

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
          const preview = detail?.documento?.textoExtraidoPreview || "";
          const p = document.createElement("p");
          p.className = "text-xs whitespace-pre-wrap";
          p.style.color = "var(--apple-text)";
          p.textContent = preview ? preview : "Aún no hay texto extraído para este documento.";
          docChunks.appendChild(p);
        }

        if (docArtefacts) {
          docArtefacts.innerHTML = "";
          const res = Array.isArray(detail.resumenes) ? detail.resumenes : [];
          const fc = Array.isArray(detail.flashcards) ? detail.flashcards : [];
          const pq = Array.isArray(detail.preguntas) ? detail.preguntas : [];

          const line1 = document.createElement("p");
          line1.className = "text-sm";
          line1.style.color = "var(--apple-text)";
          line1.textContent = `${res.length} resúmenes · ${fc.length} flashcards · ${pq.length} preguntas`;
          docArtefacts.appendChild(line1);

          if (res.length) {
            const last = res[0];
            const box = document.createElement("div");
            box.className = "mt-3 rounded-xl p-3";
            box.style.border = "1px solid var(--apple-separator)";
            box.style.background = "#ffffff";
            const t = document.createElement("p");
            t.className = "text-sm font-semibold";
            t.textContent = truncate(last.titulo || "Resumen", 90);
            const c = document.createElement("p");
            c.className = "mt-1 text-xs";
            c.style.color = "var(--apple-text-secondary)";
            c.textContent = truncate(last.contenido || "", 150);
            box.appendChild(t);
            box.appendChild(c);
            docArtefacts.appendChild(box);
          }
        }

        if (startFlashcards) startFlashcards.href = `/app/estudio/flashcards?documentoId=${documentoId}`;
        if (startTest) startTest.href = `/app/estudio/test?documentoId=${documentoId}`;

        if (genResumenBtn) genResumenBtn.onclick = () => generateArtefact("resumen", documentoId, genResumenBtn);
        if (genFlashcardsBtn) genFlashcardsBtn.onclick = () => generateArtefact("flashcards", documentoId, genFlashcardsBtn);
        if (genTestBtn) genTestBtn.onclick = () => generateArtefact("test", documentoId, genTestBtn);
      } catch (err) {
        toast(err.message || "No se pudo cargar el documento", "error");
      }
    }

    function setGeneratingState(btn, loading) {
      if (!btn) return;
      if (!btn.dataset.originalText) btn.dataset.originalText = btn.textContent || "";
      btn.disabled = !!loading;
      if (loading) {
        btn.innerHTML = `<span class="inline-flex items-center gap-2"><svg class="h-3.5 w-3.5 animate-spin" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" opacity="0.25"></circle><path d="M22 12a10 10 0 00-10-10" stroke="currentColor" stroke-width="3"></path></svg>Generando con IA...</span>`;
      } else {
        btn.textContent = btn.dataset.originalText;
      }
    }

    async function generateArtefact(type, documentoId, button) {
      if (!documentoId) return;
      setGeneratingState(button, true);
      try {
        if (type === "resumen") {
          await apiFetch(`/api/resumen/generar/documento/${documentoId}`, { method: "POST" });
        } else if (type === "flashcards") {
          await apiFetch(`/api/flashcards/generar/${documentoId}`, { method: "POST" });
        } else if (type === "test") {
          await apiFetch(`/api/preguntas/generar/${documentoId}`, { method: "POST" });
        }
        await pollDocumentoEstado(
          documentoId,
          async () => {
            const etiqueta =
              type === "resumen" ? "Resumen generado" : type === "flashcards" ? "Flashcards generadas" : "Test generado";
            toast(`✓ ${etiqueta}`, "success");
            await loadDocumentoDetalle(documentoId);
            await refreshOverview();
            setGeneratingState(button, false);
          },
          async (msg) => {
            toast(msg || "Error generando con IA", "error");
            await loadDocumentoDetalle(documentoId);
            setGeneratingState(button, false);
          }
        );
      } catch (err) {
        setGeneratingState(button, false);
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
    const uploadFileLabel = qs("upload-file-label");
    const uploadTema = qs("upload-tema");
    const temasList = qs("temas-list");
    const createTemaForm = qs("create-tema-form");
    const createTemaTitle = qs("create-tema-title");
    const docsList = qs("docs-list");
    const docsEmpty = qs("docs-empty");
    const docsCounter = qs("docs-counter");
    const refreshDocsBtn = qs("refresh-docs");
    const submitBtn = uploadForm?.querySelector('button[type="submit"]');

    let temas = [];
    let docs = [];
    let pollTimer = null;

    function fmtBytes(bytes) {
      const n = Number(bytes);
      if (!Number.isFinite(n) || n <= 0) return "—";
      const units = ["B", "KB", "MB", "GB"];
      let i = 0;
      let v = n;
      while (v >= 1024 && i < units.length - 1) {
        v /= 1024;
        i += 1;
      }
      return `${v.toFixed(v >= 10 || i === 0 ? 0 : 1)} ${units[i]}`;
    }

    function fmtFecha(iso) {
      if (!iso) return "";
      try {
        const d = new Date(iso);
        if (Number.isNaN(d.getTime())) return "";
        return d.toLocaleDateString("es-ES", { day: "2-digit", month: "short", year: "numeric" });
      } catch {
        return "";
      }
    }

    function iconForExtension(ext) {
      const e = (ext || "").toLowerCase().replace(".", "");
      if (e === "pdf") return { label: "PDF", color: "#dc2626" };
      if (e === "doc" || e === "docx") return { label: "DOC", color: "#2563eb" };
      if (e === "txt" || e === "md") return { label: "TXT", color: "#6b7280" };
      if (e === "zip") return { label: "ZIP", color: "#7c3aed" };
      return { label: e ? e.toUpperCase().slice(0, 4) : "DOC", color: "#0071e3" };
    }

    function temaTituloPorId(id) {
      if (id == null) return null;
      const t = temas.find((x) => Number(x.id) === Number(id));
      return t?.titulo || `Tema #${id}`;
    }

    function renderTemas() {
      if (!temasList) return;
      temasList.innerHTML = "";

      if (!Array.isArray(temas) || !temas.length) {
        const p = document.createElement("p");
        p.style.color = "var(--apple-text-secondary)";
        p.style.fontSize = "13px";
        p.textContent = "Sin temas todavía.";
        temasList.appendChild(p);
        return;
      }

      temas.forEach((t) => {
        const row = document.createElement("div");
        row.className = "fuentes-tema-row";

        const left = document.createElement("div");
        const title = document.createElement("p");
        title.className = "fuentes-tema-title";
        title.textContent = t.titulo || `Tema #${t.id}`;
        const meta = document.createElement("p");
        meta.className = "fuentes-tema-meta";
        meta.textContent = `ID ${t.id}`;
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

    function updateDocsCounter() {
      if (!docsCounter) return;
      const n = Array.isArray(docs) ? docs.length : 0;
      docsCounter.textContent = n === 1 ? "1 documento" : `${n} documentos`;
    }

    function renderDocs() {
      if (!docsList) return;
      docsList.innerHTML = "";
      docsEmpty?.classList.add("hidden");
      updateDocsCounter();

      if (!Array.isArray(docs) || !docs.length) {
        docsEmpty?.classList.remove("hidden");
        return;
      }

      docs.forEach((d) => {
        const estado = fmtEstadoDocumento(d.estadoProcesado);
        const icon = iconForExtension(d.extension || (d.nombreOriginal || "").split(".").pop());
        const card = document.createElement("article");
        card.className = "fuente-card";

        // Cabecera con icono, título y estado
        const head = document.createElement("div");
        head.className = "fuente-head";

        const iconBox = document.createElement("div");
        iconBox.className = "fuente-icon";
        iconBox.style.background = `${icon.color}1a`;
        iconBox.style.color = icon.color;
        iconBox.textContent = icon.label;

        const headInfo = document.createElement("div");
        headInfo.className = "fuente-head-info";

        const title = document.createElement("p");
        title.className = "fuente-title";
        title.title = d.nombreOriginal || "";
        title.textContent = d.nombreOriginal || `Documento ${d.id}`;

        const subline = document.createElement("p");
        subline.className = "fuente-subline";
        const partes = [];
        partes.push(temaTituloPorId(d.temaId) || "Sin tema");
        if (d.tamanoBytes) partes.push(fmtBytes(d.tamanoBytes));
        if (d.paginas) partes.push(`${d.paginas} pág.`);
        if (d.createdAt) partes.push(fmtFecha(d.createdAt));
        subline.textContent = partes.join(" · ");

        headInfo.appendChild(title);
        headInfo.appendChild(subline);

        const badge = document.createElement("span");
        badge.className = estadoBadgeClass(d.estadoProcesado);
        badge.textContent = estado.label;

        head.appendChild(iconBox);
        head.appendChild(headInfo);
        head.appendChild(badge);
        card.appendChild(head);

        if (d.errorExtraccion) {
          const err = document.createElement("div");
          err.className = "apple-feedback wrong";
          err.style.fontSize = "12px";
          err.style.marginTop = "10px";
          err.textContent = truncate(d.errorExtraccion, 220);
          card.appendChild(err);
        }

        // Acciones
        const actions = document.createElement("div");
        actions.className = "fuente-actions";

        const link = document.createElement("a");
        link.href = `/app/notebooks/${notebookId}`;
        link.className = "apple-link";
        link.textContent = "Abrir en notebook →";

        const right = document.createElement("div");
        right.className = "fuente-actions-right";

        const selWrap = document.createElement("div");
        selWrap.className = "fuente-select-wrap";
        const sel = document.createElement("select");
        sel.className = "fuente-select";
        sel.title = "Asignar tema";
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
          sel.disabled = true;
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
            d.temaId = temaId;
            toast("Tema actualizado", "success");
            const sub = card.querySelector(".fuente-subline");
            if (sub) {
              const ps = [];
              ps.push(temaTituloPorId(d.temaId) || "Sin tema");
              if (d.tamanoBytes) ps.push(fmtBytes(d.tamanoBytes));
              if (d.paginas) ps.push(`${d.paginas} pág.`);
              if (d.createdAt) ps.push(fmtFecha(d.createdAt));
              sub.textContent = ps.join(" · ");
            }
          } catch (err) {
            toast(err.message || "No se pudo asignar el tema", "error");
            sel.value = d.temaId ? String(d.temaId) : "";
          } finally {
            sel.disabled = false;
          }
        });
        selWrap.appendChild(sel);

        const del = document.createElement("button");
        del.type = "button";
        del.className = "apple-danger-btn";
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

        right.appendChild(selWrap);
        right.appendChild(del);
        actions.appendChild(link);
        actions.appendChild(right);
        card.appendChild(actions);

        docsList.appendChild(card);
      });
    }

    function hayProcesando() {
      return docs.some((d) => {
        const e = (d.estadoProcesado || "").toUpperCase();
        return e === "PROCESANDO" || e === "PENDIENTE";
      });
    }

    function schedulePolling() {
      if (pollTimer) {
        clearTimeout(pollTimer);
        pollTimer = null;
      }
      if (!hayProcesando()) return;
      pollTimer = setTimeout(async () => {
        try {
          await refreshDocs(true);
        } catch {
          // se reintentará
        }
      }, 4000);
    }

    async function refreshTemas() {
      try {
        temas = await apiFetch(`/api/temas?asignaturaId=${notebookId}`);
        temas = Array.isArray(temas) ? temas : [];
      } catch {
        temas = [];
      }
      renderTemas();
      fillTemaSelect();
    }

    async function refreshDocs(silencioso = false) {
      try {
        const lista = await apiFetch(`/api/documentos?asignaturaId=${notebookId}`);
        docs = Array.isArray(lista) ? lista : [];
        docs.sort((a, b) => (b.createdAt || "").localeCompare(a.createdAt || ""));
      } catch (err) {
        if (!silencioso) toast(err.message || "No se pudieron cargar documentos", "error");
        docs = [];
      }
      renderDocs();
      schedulePolling();
    }

    async function refreshTitulo() {
      try {
        const asig = await apiFetch(`/api/asignaturas/${notebookId}`);
        if (notebookTitle) notebookTitle.textContent = asig?.nombre || `Notebook ${notebookId}`;
      } catch {
        if (notebookTitle) notebookTitle.textContent = `Notebook ${notebookId}`;
      }
    }

    const uploadFileLabelText = uploadFileLabel?.querySelector(".fuentes-file-drop-text");
    if (uploadFile && uploadFileLabel) {
      uploadFile.addEventListener("change", () => {
        const f = uploadFile.files?.[0];
        if (uploadFileLabelText) {
          uploadFileLabelText.textContent = f ? f.name : "Selecciona un archivo (PDF, DOCX, TXT, MD, ZIP)";
        }
        uploadFileLabel.classList.toggle("has-file", !!f);
      });
    }

    uploadForm?.addEventListener("submit", async (e) => {
      e.preventDefault();
      const file = uploadFile?.files?.[0];
      if (!file) {
        toast("Selecciona un archivo primero", "warning");
        return;
      }
      const temaId = uploadTema?.value ? uploadTema.value : "";

      const fd = new FormData();
      fd.append("file", file);
      fd.append("asignaturaId", String(notebookId));
      if (temaId) fd.append("temaId", temaId);

      if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.dataset.originalText = submitBtn.dataset.originalText || submitBtn.textContent;
        submitBtn.textContent = "Subiendo…";
      }

      try {
        const res = await apiFetch("/api/documentos/upload", { method: "POST", body: fd });
        const warnings = res?.warnings || [];
        toast(warnings.length ? "Subido con avisos" : "Documento subido", warnings.length ? "info" : "success");
        if (warnings.length) warnings.forEach((w) => toast(w, "info"));
        uploadFile.value = "";
        if (uploadFileLabel) {
          if (uploadFileLabelText) {
            uploadFileLabelText.textContent = "Selecciona un archivo (PDF, DOCX, TXT, MD, ZIP)";
          }
          uploadFileLabel.classList.remove("has-file");
        }
        await refreshDocs();
      } catch (err) {
        toast(err.message || "No se pudo subir el documento", "error");
      } finally {
        if (submitBtn) {
          submitBtn.disabled = false;
          submitBtn.textContent = submitBtn.dataset.originalText || "Subir y procesar";
        }
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
        renderDocs();
      } catch (err) {
        toast(err.message || "No se pudo crear el tema", "error");
      }
    });

    refreshDocsBtn?.addEventListener("click", () => refreshDocs());

    document.addEventListener("visibilitychange", () => {
      if (document.visibilityState === "hidden" && pollTimer) {
        clearTimeout(pollTimer);
        pollTimer = null;
      } else if (document.visibilityState === "visible" && hayProcesando()) {
        schedulePolling();
      }
    });

    await Promise.all([refreshTitulo(), refreshTemas(), refreshDocs()]);
    renderDocs();
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
        p.style.color = "var(--apple-text-secondary)";
        p.style.fontSize = "13px";
        p.textContent = "No hay conversaciones.";
        listEl.appendChild(p);
        return;
      }

      conversations.forEach((c) => {
        const btn = document.createElement("button");
        btn.className = "apple-list-btn";
        if (c.id === activeId) btn.classList.add("is-active");
        btn.addEventListener("click", async () => {
          activeId = c.id;
          renderConversations();
          await loadMensajes();
        });
        const t = document.createElement("p");
        t.style.fontSize = "13px";
        t.style.fontWeight = "600";
        t.textContent = c.titulo || `Conversación #${c.id}`;
        const meta = document.createElement("p");
        meta.style.fontSize = "11px";
        meta.style.color = "var(--apple-text-secondary)";
        meta.style.marginTop = "2px";
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
      wrap.className = `apple-msg ${isUser ? "user" : "assistant"}`;
      const bubble = document.createElement("div");
      bubble.className = "apple-bubble";
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
        <form id="chat-new-form" style="display:flex; flex-direction:column; gap:10px;">
          <div>
            <label style="display:block; font-size:12px; color:var(--apple-text-secondary); margin-bottom:4px;">Notebook ID (asignaturaId)</label>
            <input id="chat-asignatura-id" type="number" class="apple-input" placeholder="Opcional" />
          </div>
          <div>
            <label style="display:block; font-size:12px; color:var(--apple-text-secondary); margin-bottom:4px;">Título</label>
            <input id="chat-title" class="apple-input" placeholder="Ej: Dudas" />
          </div>
          <button type="submit" class="apple-btn-primary">Crear</button>
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
        btn.className = "apple-option-btn";
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
              if (idx2 === indiceCorrecto) child.classList.add("correct");
              if (idx2 === i && !correcta) child.classList.add("wrong");
            });

            feedback.classList.remove("hidden");
            feedback.className = "apple-feedback " + (correcta ? "correct" : "wrong");
            feedback.textContent = correcta ? "Correcto." : "Incorrecto.";
            if (res.explicacion) {
              const extra = document.createElement("p");
              extra.style.marginTop = "6px";
              extra.style.fontSize = "13px";
              extra.style.opacity = "0.9";
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
          card.className = "apple-panel";
          card.style.padding = "14px";
          if (n.colorHex) card.style.borderLeft = `4px solid ${n.colorHex}`;

          const title = document.createElement("p");
          title.style.fontSize = "13px";
          title.style.fontWeight = "600";
          title.textContent = n.titulo || "Nota";

          const content = document.createElement("p");
          content.style.marginTop = "6px";
          content.style.fontSize = "12px";
          content.style.color = "var(--apple-text-secondary)";
          content.style.whiteSpace = "pre-wrap";
          content.textContent = truncate(n.contenido || "", 260);

          const meta = document.createElement("p");
          meta.style.marginTop = "10px";
          meta.style.fontSize = "11px";
          meta.style.color = "var(--apple-text-secondary)";
          meta.textContent = `Doc: ${n.documentoId ?? "—"} · Tema: ${n.temaId ?? "—"}`;

          const del = document.createElement("button");
          del.className = "apple-danger-btn";
          del.style.marginTop = "12px";
          del.style.width = "100%";
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
      const temaIdVal = Number(qs("note-tema-id")?.value) || null;

      if (!titulo || !contenido) return;

      try {
        await apiFetch("/api/notas", {
          method: "POST",
          body: {
            documentoId: docIdVal,
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

  async function initHomePage() {
    requireAuth();
    setupLogout();
    await ensureNivelEstudioSeleccionado();

    const grid = qs("asignaturas-grid");
    const showMoreWrap = qs("show-more-wrap");
    const showMoreBtn = qs("show-more-btn");
    let asignaturas = [];
    let pagina = 0;

    function renderGrid() {
      if (!grid) return;
      grid.innerHTML = "";
      const inicio = pagina * 10;
      const fin = inicio + 10;
      const visibles = asignaturas.slice(inicio, fin);

      visibles.forEach((a) => {
        const card = document.createElement("button");
        card.className = "asignatura-tile";
        card.style.borderTop = `4px solid ${normalizeHex(a.colorHex) || "#0071e3"}`;
        card.innerHTML = `
          ${a.trimestre ? `<span class="asignatura-chip">T${a.trimestre}</span>` : ""}
          <span class="asignatura-delete" title="Eliminar asignatura">×</span>
          <span class="asignatura-name">${truncate(a.nombre || "Asignatura", 60)}</span>
        `;
        card.querySelector(".asignatura-delete")?.addEventListener("click", async (event) => {
          event.preventDefault();
          event.stopPropagation();
          if (!confirm(`¿Eliminar la asignatura "${a.nombre || "sin nombre"}"?`)) return;
          try {
            await apiFetch(`/api/asignaturas/${a.id}`, { method: "DELETE" });
            toast("Asignatura eliminada", "success");
            asignaturas = asignaturas.filter((x) => x.id !== a.id);
            if (pagina > 0 && pagina * 10 >= asignaturas.length) pagina -= 1;
            renderGrid();
          } catch (err) {
            toast(err.message || "No se pudo eliminar la asignatura", "error");
          }
        });
        card.addEventListener("click", () => {
          window.location.href = `/app/home/${a.id}/trimestres`;
        });
        grid.appendChild(card);
      });

      const slotsVacios = Math.max(0, 10 - visibles.length);
      for (let i = 0; i < slotsVacios; i += 1) {
        const empty = document.createElement("button");
        empty.className = "asignatura-empty";
        empty.innerHTML = '<span class="plus">+</span>';
        empty.addEventListener("click", openCrearAsignaturaModal);
        grid.appendChild(empty);
      }

      const hayMas = fin < asignaturas.length;
      if (showMoreWrap) showMoreWrap.style.display = hayMas ? "flex" : "none";
    }

    function openCrearAsignaturaModal() {
      const close = openModal(
        "Crear asignatura",
        `
        <form id="form-crear-asignatura" class="space-y-3">
          <input id="asig-nombre" class="apple-input" placeholder="Ej: Matemáticas" required />
          <textarea id="asig-descripcion" class="apple-input" rows="3" placeholder="Descripción (opcional)"></textarea>
          <label style="display:block; font-size:12px; color:var(--apple-text-secondary); margin-bottom:4px;">Color identificativo</label>
          <input id="asig-color" type="color" value="#0071e3" class="h-10 w-full" />
          <select id="asig-trimestre" class="apple-input">
            <option value="">Sin asignar</option>
            <option value="1">${isUniversitario() ? "1er Cuatrimestre" : "1er Trimestre"}</option>
            <option value="2">${isUniversitario() ? "2º Cuatrimestre" : "2º Trimestre"}</option>
            ${isUniversitario() ? "" : '<option value="3">3er Trimestre</option>'}
          </select>
          <button class="apple-btn-primary w-full" type="submit">Crear asignatura</button>
        </form>
      `
      );

      qs("form-crear-asignatura")?.addEventListener("submit", async (e) => {
        e.preventDefault();
        const nombre = qs("asig-nombre")?.value?.trim();
        if (!nombre) return;
        const trimestreRaw = qs("asig-trimestre")?.value;
        try {
          await apiFetch("/api/asignaturas", {
            method: "POST",
            body: {
              nombre,
              descripcion: qs("asig-descripcion")?.value?.trim() || null,
              colorHex: qs("asig-color")?.value || "#0071e3",
              trimestre: trimestreRaw ? Number(trimestreRaw) : null,
            },
          });
          close();
          toast("Asignatura creada", "success");
          asignaturas = await apiFetch("/api/asignaturas");
          pagina = 0;
          renderGrid();
        } catch (err) {
          toast(err.message || "No se pudo crear la asignatura", "error");
        }
      });
    }

    showMoreBtn?.addEventListener("click", () => {
      pagina += 1;
      renderGrid();
    });

    try {
      asignaturas = await apiFetch("/api/asignaturas");
      asignaturas = Array.isArray(asignaturas) ? asignaturas : [];
      renderGrid();
    } catch (err) {
      toast(err.message || "No se pudieron cargar asignaturas", "error");
    }
  }

  async function initTrimestresPage() {
    requireAuth();
    setupLogout();
    const asignaturaId = Number(document.body.dataset.asignaturaId);
    if (!asignaturaId) return;

    const title = qs("asignatura-title");
    const crumb = qs("breadcrumb-asignatura");
    const dot = qs("asignatura-dot");
    const grid = qs("trimestres-grid");
    const fab = qs("fab-add");
    let asignatura = null;

    function colorToRgba(hex, alpha) {
      const h = (normalizeHex(hex) || "#0071e3").replace("#", "");
      const full = h.length === 3 ? h.split("").map((x) => x + x).join("") : h;
      const n = Number.parseInt(full, 16);
      const r = (n >> 16) & 255;
      const g = (n >> 8) & 255;
      const b = n & 255;
      return `rgba(${r},${g},${b},${alpha})`;
    }

    function openCrearTemaModal() {
      const close = openModal(
        "Nuevo tema",
        `
        <form id="form-crear-tema-trimestres" class="space-y-3">
          <input id="tema-titulo" class="apple-input" placeholder="Título" required />
          <textarea id="tema-desc" class="apple-input" rows="3" placeholder="Descripción (opcional)"></textarea>
          <input id="tema-palabras" class="apple-input" placeholder="Palabras clave (coma separadas)" />
          <select id="tema-trimestre" class="apple-input">
            <option value="1">${isUniversitario() ? "1er Cuatrimestre" : "1er Trimestre"}</option>
            <option value="2">${isUniversitario() ? "2º Cuatrimestre" : "2º Trimestre"}</option>
            ${isUniversitario() ? "" : '<option value="3">3er Trimestre</option>'}
            <option value="0">General</option>
          </select>
          <button class="apple-btn-primary w-full" type="submit">Crear tema</button>
        </form>
      `
      );

      qs("form-crear-tema-trimestres")?.addEventListener("submit", async (e) => {
        e.preventDefault();
        try {
          await apiFetch("/api/temas/rapido", {
            method: "POST",
            body: {
              asignaturaId,
              trimestre: Number(qs("tema-trimestre")?.value || "0"),
              titulo: qs("tema-titulo")?.value?.trim(),
              descripcion: qs("tema-desc")?.value?.trim() || null,
              palabrasClave: qs("tema-palabras")?.value?.trim() || null,
            },
          });
          close();
          toast("Tema creado", "success");
        } catch (err) {
          toast(err.message || "No se pudo crear el tema", "error");
        }
      });
    }

    try {
      const [asig, resumen] = await Promise.all([
        apiFetch(`/api/asignaturas/${asignaturaId}`),
        apiFetch(`/api/asignaturas/${asignaturaId}/resumen-trimestres`),
      ]);
      asignatura = asig;
      const color = normalizeHex(asig.colorHex) || "#0071e3";
      if (title) title.textContent = asig.nombre || "Asignatura";
      if (crumb) crumb.textContent = asig.nombre || "Asignatura";
      if (dot) dot.style.background = color;

      const cards = isUniversitario()
        ? [
            { t: 1, code: "C1", nombre: "Primer cuatrimestre", count: resumen?.trimestre1 || 0 },
            { t: 2, code: "C2", nombre: "Segundo cuatrimestre", count: resumen?.trimestre2 || 0 },
            { t: 0, code: "·", nombre: "General", count: resumen?.sinAsignar || 0 },
          ]
        : [
            { t: 1, code: "T1", nombre: "Primer trimestre", count: resumen?.trimestre1 || 0 },
            { t: 2, code: "T2", nombre: "Segundo trimestre", count: resumen?.trimestre2 || 0 },
            { t: 3, code: "T3", nombre: "Tercer trimestre", count: resumen?.trimestre3 || 0 },
            { t: 0, code: "·", nombre: "General", count: resumen?.sinAsignar || 0 },
          ];
      grid.innerHTML = "";
      cards.forEach((c) => {
        const card = document.createElement("button");
        card.className = "trimestre-card";
        card.style.background = `linear-gradient(180deg, ${colorToRgba(color, 0.08)}, #ffffff)`;
        card.style.borderColor = colorToRgba(color, 0.2);
        card.innerHTML = `
          <p class="trimestre-num" style="color:${colorToRgba(color, 0.5)};">${c.code}</p>
          <p class="trimestre-name">${c.nombre}</p>
          <p class="trimestre-count">${c.count} temas</p>
        `;
        card.addEventListener("click", () => {
          window.location.href = `/app/home/${asignaturaId}/trimestre/${c.t}/temas`;
        });
        grid.appendChild(card);
      });
    } catch (err) {
      toast(err.message || "No se pudieron cargar trimestres", "error");
    }

    fab?.addEventListener("click", openCrearTemaModal);
  }

  async function initTemasPage() {
    requireAuth();
    setupLogout();
    const asignaturaId = Number(document.body.dataset.asignaturaId);
    const trimestre = Number(document.body.dataset.trimestre);
    if (!asignaturaId && asignaturaId !== 0) return;

    const list = qs("temas-list");
    const empty = qs("temas-empty");
    const search = qs("temas-search");
    const title = qs("page-title");
    const breadcrumbAsig = qs("breadcrumb-asignatura");
    const breadcrumbTrim = qs("breadcrumb-trimestre");
    const fabMenu = qs("fab-menu");
    const fabMain = qs("fab-main");
    const fabNewTema = qs("fab-new-tema");
    const fabUploadDoc = qs("fab-upload-doc");
    const fabNewUnidad = qs("fab-new-unidad");
    const btnAddFirstTema = qs("btn-add-first-tema");
    let temas = [];
    let temasFiltrados = [];

    function nombreTrimestre(t) {
      return etiquetaPeriodo(t);
    }

    function renderTemas() {
      if (!list) return;
      list.innerHTML = "";
      if (!temasFiltrados.length) {
        empty.style.display = "block";
        return;
      }
      empty.style.display = "none";
      temasFiltrados.forEach((t) => {
        const row = document.createElement("div");
        row.className = "tema-row";
        row.innerHTML = `
          <div>
            <p class="tema-title">${truncate(t.titulo || "Tema", 120)}</p>
            ${t.descripcion ? `<p class="tema-desc">${truncate(t.descripcion, 160)}</p>` : ""}
          </div>
          <div class="tema-chips">
            <span class="apple-chip">📄 ${t.documentosCount || 0}</span>
            <span class="apple-chip">🃏 ${t.flashcardsCount || 0}</span>
            <span class="apple-chip">✅ ${t.preguntasCount || 0}</span>
            <button class="tema-delete-chip" title="Eliminar tema">Eliminar</button>
          </div>
        `;
        row.querySelector(".tema-delete-chip")?.addEventListener("click", async (event) => {
          event.preventDefault();
          event.stopPropagation();
          if (!confirm(`¿Eliminar el tema "${t.titulo || "sin nombre"}"?`)) return;
          try {
            await apiFetch(`/api/temas/${t.id}`, { method: "DELETE" });
            toast("Tema eliminado", "success");
            temas = temas.filter((x) => x.id !== t.id);
            filtrar(search?.value || "");
          } catch (err) {
            toast(err.message || "No se pudo eliminar el tema", "error");
          }
        });
        row.addEventListener("click", () => {
          window.location.href = `/app/notebooks/${asignaturaId}?temaId=${t.id}`;
        });
        list.appendChild(row);
      });
    }

    function filtrar(query = "") {
      const q = query.trim().toLowerCase();
      temasFiltrados = temas.filter((t) => {
        const triTema = t.trimestre == null ? 0 : Number(t.trimestre);
        if (triTema !== trimestre) {
          return false;
        }
        if (!q) return true;
        return (
          (t.titulo || "").toLowerCase().includes(q) ||
          (t.descripcion || "").toLowerCase().includes(q) ||
          (t.palabrasClave || "").toLowerCase().includes(q)
        );
      });
      renderTemas();
    }

    function toggleFabMenu(force = null) {
      if (!fabMenu) return;
      const open = force == null ? fabMenu.dataset.open !== "true" : !!force;
      fabMenu.dataset.open = open ? "true" : "false";
    }

    function openCrearTemaModal() {
      toggleFabMenu(false);
      const close = openModal(
        "Nuevo tema",
        `
        <form id="form-crear-tema" class="space-y-3">
          <input id="nt-titulo" class="apple-input" placeholder="Título" required />
          <textarea id="nt-desc" class="apple-input" rows="3" placeholder="Descripción (opcional)"></textarea>
          <input id="nt-tags" class="apple-input" placeholder="Palabras clave separadas por coma" />
          <button class="apple-btn-primary w-full" type="submit">Guardar tema</button>
        </form>
      `
      );
      qs("form-crear-tema")?.addEventListener("submit", async (e) => {
        e.preventDefault();
        try {
          const nuevo = await apiFetch("/api/temas/rapido", {
            method: "POST",
            body: {
              asignaturaId,
              trimestre,
              titulo: qs("nt-titulo")?.value?.trim(),
              descripcion: qs("nt-desc")?.value?.trim() || null,
              palabrasClave: qs("nt-tags")?.value?.trim() || null,
            },
          });
          close();
          toast("Tema creado", "success");
          temas.unshift({
            ...nuevo,
            trimestre: trimestre === 0 ? null : trimestre,
            documentosCount: 0,
            flashcardsCount: 0,
            preguntasCount: 0,
          });
          filtrar(search?.value || "");
        } catch (err) {
          toast(err.message || "No se pudo crear el tema", "error");
        }
      });
    }

    function openSubirDocumentoModal() {
      toggleFabMenu(false);
      const opciones = temasFiltrados
        .map((t) => `<option value="${t.id}">${truncate(t.titulo || `Tema ${t.id}`, 80)}</option>`)
        .join("");
      const close = openModal(
        "Subir documento",
        `
        <form id="form-subir-documento" class="space-y-3">
          <select id="doc-tema" class="apple-input" required>
            <option value="">Selecciona tema</option>
            ${opciones}
          </select>
          <input id="doc-file" type="file" class="apple-input" required />
          <button class="apple-btn-primary w-full" type="submit">Subir documento</button>
        </form>
      `
      );
      qs("form-subir-documento")?.addEventListener("submit", async (e) => {
        e.preventDefault();
        const file = qs("doc-file")?.files?.[0];
        const temaId = qs("doc-tema")?.value;
        if (!file || !temaId) return;
        const fd = new FormData();
        fd.append("file", file);
        fd.append("asignaturaId", String(asignaturaId));
        fd.append("temaId", String(temaId));
        try {
          await apiFetch("/api/documentos/upload", { method: "POST", body: fd });
          close();
          toast("Documento subido", "success");
        } catch (err) {
          toast(err.message || "No se pudo subir el documento", "error");
        }
      });
    }

    try {
      const asig = await apiFetch(`/api/asignaturas/${asignaturaId}`);
      breadcrumbAsig.textContent = asig.nombre || "Asignatura";
      breadcrumbAsig.href = `/app/home/${asignaturaId}/trimestres`;
      breadcrumbTrim.textContent = nombreTrimestre(trimestre);
      title.textContent = `${asig.nombre || "Asignatura"} · ${nombreTrimestre(trimestre)}`;

      const temasPlanos = await apiFetch(`/api/asignaturas/${asignaturaId}/temas-planos`);
      temas = Array.isArray(temasPlanos) ? temasPlanos : [];
      filtrar("");
    } catch (err) {
      toast(err.message || "No se pudieron cargar temas", "error");
    }

    search?.addEventListener("input", () => filtrar(search.value || ""));
    fabMain?.addEventListener("click", () => toggleFabMenu());
    fabNewTema?.addEventListener("click", openCrearTemaModal);
    btnAddFirstTema?.addEventListener("click", openCrearTemaModal);
    fabUploadDoc?.addEventListener("click", openSubirDocumentoModal);
    fabNewUnidad?.addEventListener("click", () => {
      toggleFabMenu(false);
      toast("La creación de unidades se añadirá en una siguiente iteración.", "info");
    });
    document.addEventListener("click", (e) => {
      if (!fabMenu) return;
      if (!fabMenu.contains(e.target) && fabMenu.dataset.open === "true") {
        toggleFabMenu(false);
      }
    });
  }

  function setupFabAnimations() {
    document.addEventListener("click", (event) => {
      const fab = event.target.closest(".apple-fab, .fab-mini-btn");
      if (!fab) return;
      fab.classList.remove("is-pressed");
      void fab.offsetWidth;
      fab.classList.add("is-pressed");
      setTimeout(() => fab.classList.remove("is-pressed"), 360);
    });
  }

  async function init() {
    setupFabAnimations();

    const page = document.body.dataset.page;
    if (!page) return;

    if (page !== "login") requireAuth();
    checkTokenExpiry();

    if (page === "login") return initLogin();
    if (page === "home-index") return initHomePage();
    if (page === "home-trimestres") return initTrimestresPage();
    if (page === "home-temas") return initTemasPage();
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
