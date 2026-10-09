import utilities from "./utilities.module.css";
import "../style.css";

const mapUtilityClasses = (node) => {
  if (!(node instanceof Element)) return;

  const elements = [node, ...node.querySelectorAll("[class]")];
  elements.forEach((element) => {
    [...element.classList].forEach((className) => {
      const scopedClass = utilities[className];
      if (scopedClass) {
        element.classList.replace(className, scopedClass);
      }
    });
  });
};

mapUtilityClasses(document.documentElement);

const utilityObserver = new MutationObserver((records) => {
  records.forEach((record) => {
    record.addedNodes.forEach(mapUtilityClasses);
  });
});

utilityObserver.observe(document.documentElement, {
  childList: true,
  subtree: true,
});
