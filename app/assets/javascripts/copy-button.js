(function () {
  function copy() {
    const code = document.getElementById("copy-to-clip").dataset.code;


    // Create text area
    var textArea = document.createElement("textarea");
    textArea.style.position = 'fixed';
    textArea.style.top = 0;
    textArea.style.left = 0;
    textArea.style.width = '2em';
    textArea.style.height = '2em';
    textArea.style.padding = 0;
    textArea.style.border = 'none';
    textArea.style.outline = 'none';
    textArea.style.boxShadow = 'none';
    textArea.style.background = 'transparent';
    textArea.value = code;

    document.body.appendChild(textArea);

    textArea.select();
    document.execCommand("copy");
    document.body.removeChild(textArea);
  }
  document.querySelector("#copy-to-clip-button").addEventListener("click", copy);
})();