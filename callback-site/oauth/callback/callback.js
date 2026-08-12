(() => {
  const incoming = new URL(window.location.href);
  const destination = new URL("repoglance://oauth/callback");
  const states = incoming.searchParams.getAll("state");
  const codes = incoming.searchParams.getAll("code");
  const errors = incoming.searchParams.getAll("error");
  const hasExactlyOneOutcome = (codes.length === 1) !== (errors.length === 1);

  const returnLink = document.querySelector("#return-to-app");
  const status = document.querySelector("#status");
  if (states.length !== 1 || !states[0] || !hasExactlyOneOutcome) {
    status.textContent = "This return link is incomplete. Start Connect GitHub again from RepoGlance.";
    return;
  }
  destination.searchParams.set("state", states[0]);
  if (codes.length === 1 && codes[0]) {
    destination.searchParams.set("code", codes[0]);
  } else if (errors.length === 1 && errors[0]) {
    destination.searchParams.set("error", errors[0]);
  } else {
    status.textContent = "This return link is incomplete. Start Connect GitHub again from RepoGlance.";
    return;
  }
  returnLink.href = destination.href;
  returnLink.hidden = false;
  window.location.replace(destination.href);
})();
